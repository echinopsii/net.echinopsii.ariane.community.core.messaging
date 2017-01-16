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
import akka.actor.Props;
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.SyncSubscription;
import net.echinopsii.ariane.community.messaging.api.*;
import net.echinopsii.ariane.community.messaging.api.MomLogger;
import net.echinopsii.ariane.community.messaging.common.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * ServiceFactory class extending {@link net.echinopsii.ariane.community.messaging.common.MomAkkaAbsServiceFactory} abstract class
 * and implements {@link net.echinopsii.ariane.community.messaging.api.MomServiceFactory} for NATS MoM.
 */
public class ServiceFactory extends MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {
    private static final Logger log = MomLoggerFactory.getLogger(ServiceFactory.class);

    private static MsgTranslator translator = new MsgTranslator();

    public ServiceFactory(Client client) {
        super(client);
    }

    /**
     * (internal usage)
     * Create a new request router in charge of spawning request workers
     *
     * @param source request source queue
     * @param client initialized NATS Client
     * @param requestCB application message worker to treat request
     * @param supervisor actor supervisor
     * @param nbRoutees number of routees to be managed by the new router
     * @param cache tell if reply must be cached in case of retry
     * @return the request router ActorRef
     */
    private static ActorRef createRequestRouter(String source, MomClient client, AppMsgWorker requestCB, ActorRef supervisor, int nbRoutees, boolean cache) {
        Props routeeProps = MsgRequestActor.props(((Client) client), requestCB, cache);
        String routeeNamePrefix = source + "_msgWorker";
        ActorRef sup = supervisor;
        if (sup == null) sup = ((Client)client).getMainSupervisor();
        return MomAkkaSupervisor.createNewSupervisedService(
                sup,
                MomAkkaRequestRouter.props(routeeProps, routeeNamePrefix, nbRoutees),
                source + "_router"
        );
    }

    /**
     * (internal usage)
     * Create a new request router in charge of spawning request workers. Number of request workers is defined in the client configuration.
     *
     * @param source request source queue
     * @param client initialized NATS Client
     * @param requestCB application message worker to treat request
     * @param supervisor actor supervisor
     * @param cache tell if reply must be cached in case of retry
     * @return the request router ActorRef
     */
    private static ActorRef createRequestRouter(String source, MomClient client, AppMsgWorker requestCB, ActorRef supervisor, boolean cache) {
        return createRequestRouter(source, client, requestCB, supervisor, ((Client) client).getRouteesCountPerService(), cache);
    }

    /**
     * (internal usage)
     * Create a new MomConsumer to consume message from NATS source and forward them to the request actor.
     *
     * @param source request source queue
     * @param requestActor request actor ref to treat the message
     * @param client initialized NATS Client
     * @return the new MomConsumer
     */
    private static MomConsumer createConsumer(final String source, final ActorRef requestActor, final MomClient client) {
        return new MomConsumer() {
            private boolean isRunning = false;
            private Connection connection = ((Client)client).getConnection();

            @Override
            public void run() {
                SyncSubscription subs = null;
                try {
                    subs = connection.subscribeSync(source);
                    isRunning = true;
                    Map<String, Object> finalMessage;

                    while (isRunning) {
                        finalMessage = null;
                        try {
                            Message msg = subs.nextMessage(10);
                            if (msg!=null && isRunning) {
                                finalMessage = translator.decode(new Message[]{msg});
                                if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE) && client.isMsgDebugOnTimeout())
                                    ((MomLogger)log).setMsgTraceLevel(true);
                                ((MomLogger)log).traceMessage("MomConsumer(" + source + ").run", finalMessage);
                                requestActor.tell(msg, null);
                            }
                            if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE) && client.isMsgDebugOnTimeout())
                                ((MomLogger)log).setMsgTraceLevel(false);
                        } catch (TimeoutException e) {
                            if (finalMessage!=null && client.isMsgDebugOnTimeout() &&
                                    ((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setMsgTraceLevel(false);
                            log.debug("no message found during last 10 ms");
                        } catch (IllegalStateException | IOException | InterruptedException e) {
                            if (finalMessage!=null && client.isMsgDebugOnTimeout() &&
                                    ((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setMsgTraceLevel(false);
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

    /**
     * (internal usage)
     * Create new messages group service manage to handle messages group on calling service.
     *
     * @param source request source queue
     * @param requestCB application message worker to treat request
     * @param client initialized NATS Client
     * @return the fresh new MomMsgGroupServiceMgr
     */
    private static MomMsgGroupServiceMgr createMsgGroupServiceManager(final String source, final AppMsgWorker requestCB, final MomClient client) {
        return new MomMsgGroupServiceMgr() {
            HashMap<String, MomConsumer> msgGroupConsumersRegistry = new HashMap<>();
            HashMap<String, ActorRef> msgGroupActorRegistry = new HashMap<>();

            @Override
            public void openMsgGroupService(String groupID) {
                final String sessionSource = groupID + "-" + source;
                ActorRef msgGroupSupervisor = ((Client)client).getMsgGroupSupervisor(groupID);
                ActorRef runnableReqActor;
                if (msgGroupSupervisor!=null) runnableReqActor = ServiceFactory.createRequestRouter(source, client, requestCB, msgGroupSupervisor, 2, true);
                else {
                    log.warn("No supervisor found for group " + groupID + ". Use main mom supervisor.");
                    runnableReqActor = ServiceFactory.createRequestRouter(sessionSource, client, requestCB, ((Client)client).getMainSupervisor(), 2, true);
                }
                msgGroupActorRegistry.put(groupID, runnableReqActor);
                msgGroupConsumersRegistry.put(groupID, ServiceFactory.createConsumer(sessionSource, runnableReqActor, client));
                msgGroupConsumersRegistry.get(groupID).start();
            }

            @Override
            public void closeMsgGroupService(String groupID) {
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
                for (String groupID : msgGroupConsumersRegistryClone.keySet())
                    this.closeMsgGroupService(groupID);
            }
        };
    }

    /**
     * Create a message group request service.
     *
     * @param source the source where request are coming from
     * @param requestWorker the application request worker
     * @return the new message group request service
     */
    @Override
    public MomAkkaService msgGroupRequestService(String source, AppMsgWorker requestWorker) {
        final Connection connection   = ((Client)super.getMomClient()).getConnection();

        MomAkkaService ret = null;
        ActorRef requestActor;
        MomConsumer consumer ;
        MomMsgGroupServiceMgr msgGroupServiceMgr;

        if (connection != null && !connection.isClosed()) {
            requestActor = ServiceFactory.createRequestRouter(source, super.getMomClient(), requestWorker, null, true);
            consumer = ServiceFactory.createConsumer(source, requestActor, super.getMomClient());
            consumer.start();
            msgGroupServiceMgr = ServiceFactory.createMsgGroupServiceManager(source, requestWorker, super.getMomClient());
            ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient((Client) super.getMomClient()).
                    setMsgGroupServiceMgr(msgGroupServiceMgr);
            super.getServices().add(ret);
        }
        return ret;
    }

    /**
     * Create a request service (can not handle message grouping)
     *
     * @param source the source where request are coming from
     * @param requestWorker the application request worker
     * @return the new request service
     */
    @Override
    public MomAkkaService requestService(String source, AppMsgWorker requestWorker) {
        final Connection connection   = ((Client)super.getMomClient()).getConnection();

        MomAkkaService ret = null;
        ActorRef requestActor;
        MomConsumer consumer;

        if (connection != null && !connection.isClosed()) {
            requestActor = ServiceFactory.createRequestRouter(source, super.getMomClient(), requestWorker, null, false);
            consumer = ServiceFactory.createConsumer(source, requestActor, super.getMomClient());
            consumer.start();

            ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient(super.getMomClient());
            super.getServices().add(ret);
        }

        return ret;
    }

    /**
     * Create a feeder service.
     *
     * @param baseDestination the baseDestination (must be a topic)
     * @param selector the selector of this feeder (can be null)
     * @param interval the interval (seconds) between two message feed
     * @param feederWorker the worker in charge of creating new message for feeds
     * @return the new feeder service
     */
    @Override
    public MomAkkaService feederService(String baseDestination, String selector, int interval, AppMsgFeeder feederWorker) {
        MomAkkaService ret = null;
        Connection  connection   = ((Client)super.getMomClient()).getConnection();
        if (connection != null && !connection.isClosed()) {
            ActorRef feeder = super.getMomClient().getActorSystem().actorOf(MsgFeederActor.props(
                            ((Client) super.getMomClient()), baseDestination, selector, feederWorker)
            );
            ret = new MomAkkaService().setClient(super.getMomClient()).setMsgFeeder(feeder, interval);
            super.getServices().add(ret);
        }
        return ret;
    }

    /**
     * Create a new subscriber service.
     *
     * @param baseSource the feed base source
     * @param selector the selector on the feed source (can be null)
     * @param feedWorker the feed message worker
     * @return the new subscriber service
     */
    @Override
    public MomAkkaService subscriberService(String baseSource, String selector, AppMsgWorker feedWorker) {
        MomAkkaService ret = null;
        ActorRef    subsActor ;
        MomConsumer consumer  ;
        final Connection connection = ((Client)super.getMomClient()).getConnection();

        if (connection != null && !connection.isClosed()) {
            final String subject = baseSource + ((selector !=null && !selector.equals("")) ? "." + selector : ".*");
            subsActor = super.getMomClient().getActorSystem().actorOf(MsgSubsActor.props(feedWorker), subject + "_msgWorker");
            consumer = ServiceFactory.createConsumer(subject, subsActor, super.getMomClient());
            consumer.start();
            ret = new MomAkkaService().setMsgWorker(subsActor).setConsumer(consumer).setClient(super.getMomClient());
            super.getServices().add(ret);
        }
        return ret;
    }
}
