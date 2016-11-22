/**
 * Messaging - NATS Implementation
 * ServiceFactory implementation
 * Copyright (C) 4/30/16 echinopsii
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.echinopsii.ariane.community.messaging.nats;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.SyncSubscription;
import net.echinopsii.ariane.community.messaging.api.*;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsServiceFactory;
import net.echinopsii.ariane.community.messaging.common.MomAkkaService;
import net.echinopsii.ariane.community.messaging.common.MomAkkaSupervisor;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class ServiceFactory extends MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {
    private static final Logger log = MomLoggerFactory.getLogger(ServiceFactory.class);

    private static MsgTranslator translator = new MsgTranslator();

    public ServiceFactory(Client client) {
        super(client);
    }

    private static ActorRef createRequestActor(String source, MomClient client, AppMsgWorker requestCB, ActorRef supervisor) {
        ActorRef sup = supervisor;
        if (sup == null) sup = ((Client)client).getMainSupervisor();
        return MomAkkaSupervisor.createNewSupervisedService(
                sup, MsgRequestActor.props(((Client) client), requestCB),
                source + "_msgWorker"
        );
    }

    private static MomConsumer createConsumer(final String source, final ActorRef runnableReqActor, final Connection connection) {
        return new MomConsumer() {
            private boolean isRunning = false;

            @Override
            public void run() {
                SyncSubscription subs = null;
                try {
                    subs = connection.subscribeSync(source);
                    isRunning = true;

                    while (isRunning) {
                        Map<String, Object> finalMessage = null;
                        try {
                            Message msg = subs.nextMessage(10);
                            finalMessage = translator.decode(msg);
                            if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setTraceLevel(true);
                            ((MomLogger)log).traceMessage("MomConsumer(" + source + ").run", finalMessage);
                            if (msg!=null && isRunning) runnableReqActor.tell(msg, null);
                            if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setTraceLevel(false);
                        } catch (TimeoutException e) {
                            if (finalMessage!=null &&
                                    ((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setTraceLevel(false);
                            log.debug("no message found during last 10 ms");
                        } catch (IllegalStateException | IOException | InterruptedException e) {
                            if (finalMessage!=null &&
                                    ((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setTraceLevel(false);
                            if (isRunning) log.error("[source: " + source + "]" + e.getMessage());
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (!connection.isClosed() && subs!=null) {
                        try {
                            subs.unsubscribe();
                            subs.close();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public boolean isRunning() {
                return isRunning;
            }

            @Override
            public void start() {
                new Thread(this, source + "_consumer").start();
            }

            @Override
            public void stop() {
                isRunning = false;
            }
        };
    }

    private static MomMsgGroupSubServiceMgr createMsgGroupServiceManager(final String source, final AppMsgWorker requestCB, final MomClient client) {
        return new MomMsgGroupSubServiceMgr() {
            Connection connection   = ((Client)client).getConnection();
            HashMap<String, MomConsumer> msgGroupConsumersRegistry = new HashMap<>();
            HashMap<String, ActorRef> msgGroupActorRegistry = new HashMap<>();

            @Override
            public void openMsgGroupSubService(String groupID) {
                final String sessionSource = groupID + "-" + source;
                ActorRef msgGroupSupervisor = ((Client)client).getMsgGroupSupervisor(groupID);
                ActorRef runnableReqActor = null;
                if (msgGroupSupervisor!=null) runnableReqActor = ServiceFactory.createRequestActor(source, client, requestCB, msgGroupSupervisor);
                else {
                    log.warn("No supervisor found for group " + groupID + ". Use main mom supervisor.");
                    runnableReqActor = ServiceFactory.createRequestActor(sessionSource, client, requestCB, ((Client)client).getMainSupervisor());
                }
                msgGroupActorRegistry.put(groupID, runnableReqActor);
                msgGroupConsumersRegistry.put(groupID, ServiceFactory.createConsumer(sessionSource, runnableReqActor, connection));
                msgGroupConsumersRegistry.get(groupID).start();
            }

            @Override
            public void closeMsgGroupSubService(String groupID) {
                if (msgGroupConsumersRegistry.containsKey(groupID)) {
                    ActorRef msgGroupSupervisor = ((Client)client).getMsgGroupSupervisor(groupID);
                    msgGroupConsumersRegistry.get(groupID).stop();
                    msgGroupConsumersRegistry.remove(groupID);
                    ((Client)client).getActorSystem().stop(msgGroupActorRegistry.get(groupID));
                    if (!msgGroupActorRegistry.get(groupID).isTerminated() && msgGroupSupervisor==null)
                        msgGroupActorRegistry.get(groupID).tell(PoisonPill.getInstance(), null);
                    msgGroupActorRegistry.remove(groupID);
                }
            }

            @Override
            public void stop() {
                HashMap<String, MomConsumer> msgGroupConsumersRegistryClone = (HashMap<String, MomConsumer>) msgGroupConsumersRegistry.clone();
                for (String groupID : msgGroupConsumersRegistryClone.keySet()) {
                    ActorRef msgGroupSupervisor = ((Client)client).getMsgGroupSupervisor(groupID);
                    msgGroupConsumersRegistry.get(groupID).stop();
                    msgGroupConsumersRegistry.remove(groupID);
                    ((Client)client).getActorSystem().stop(msgGroupActorRegistry.get(groupID));
                    if (!msgGroupActorRegistry.get(groupID).isTerminated() && msgGroupSupervisor==null)
                        msgGroupActorRegistry.get(groupID).tell(PoisonPill.getInstance(), null);
                    msgGroupActorRegistry.remove(groupID);
                }
            }
        };
    }

    @Override
    public MomAkkaService msgGroupRequestService(String source, AppMsgWorker requestCB) {
        final Connection connection   = ((Client)super.getMomClient()).getConnection();

        MomAkkaService ret = null;
        ActorRef requestActor;
        MomConsumer consumer ;
        MomMsgGroupSubServiceMgr msgGroupSubServiceMgr = null;

        if (connection != null && !connection.isClosed()) {
            requestActor = ServiceFactory.createRequestActor(source, super.getMomClient(), requestCB, null);
            consumer = ServiceFactory.createConsumer(source, requestActor, connection);
            consumer.start();
            msgGroupSubServiceMgr = ServiceFactory.createMsgGroupServiceManager(source, requestCB, super.getMomClient());
            ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient((Client) super.getMomClient()).
                    setMsgGroupSubServiceMgr(msgGroupSubServiceMgr);
            super.getServices().add(ret);
        }
        return ret;
    }

    @Override
    public MomAkkaService requestService(String source, AppMsgWorker requestCB) {
        final Connection connection   = ((Client)super.getMomClient()).getConnection();

        MomAkkaService ret    = null;
        ActorRef requestActor = null;
        MomConsumer consumer  = null;

        if (connection != null && !connection.isClosed()) {
            requestActor = ServiceFactory.createRequestActor(source, super.getMomClient(), requestCB, null);
            consumer = ServiceFactory.createConsumer(source, requestActor, connection);
            consumer.start();

            ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient(
                    ((Client) super.getMomClient())
            );
            super.getServices().add(ret);
        }

        return ret;
    }

    @Override
    public MomAkkaService feederService(String baseDestination, String selector, int interval, AppMsgFeeder feederCB) {
        MomAkkaService ret = null;
        Connection  connection   = ((Client)super.getMomClient()).getConnection();
        if (connection != null && !connection.isClosed()) {
            ActorRef feeder = ((Client)super.getMomClient()).getActorSystem().actorOf(MsgFeederActor.props(
                            ((Client)super.getMomClient()),baseDestination, selector, feederCB)
            );
            ret = new MomAkkaService().setClient(((Client) super.getMomClient())).setMsgFeeder(feeder, interval);
            super.getServices().add(ret);
        }
        return ret;
    }

    @Override
    public MomAkkaService subscriberService(String source, String selector, AppMsgWorker feedCB) {
        MomAkkaService ret    = null;
        ActorRef    subsActor ;
        MomConsumer consumer  ;
        final Connection connection = ((Client)super.getMomClient()).getConnection();

        if (connection != null && !connection.isClosed()) {
            final String subject = source + ((selector !=null && !selector.equals("")) ? "." + selector : "");
            subsActor = ((Client)super.getMomClient()).getActorSystem().actorOf(
                    MsgSubsActor.props(feedCB), subject + "_msgWorker"
            );
            consumer = ServiceFactory.createConsumer(subject, subsActor, connection);
            consumer.start();

            ret = new MomAkkaService().setMsgWorker(subsActor).setConsumer(consumer).setClient(
                    ((Client)super.getMomClient())
            );
            super.getServices().add(ret);
        }
        return ret;
    }
}
