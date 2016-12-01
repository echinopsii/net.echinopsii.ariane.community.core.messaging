/**
 * Messaging - RabbitMQ Implementation
 * Service implementation
 * Copyright (C) 8/24/14 echinopsii
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

package net.echinopsii.ariane.community.messaging.rabbitmq;

import akka.actor.ActorRef;
import akka.actor.PoisonPill;
import akka.actor.Props;
import com.rabbitmq.client.*;
import net.echinopsii.ariane.community.messaging.api.*;
import net.echinopsii.ariane.community.messaging.api.MomLogger;
import net.echinopsii.ariane.community.messaging.common.*;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ServiceFactory extends MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {

    private static final Logger log = MomLoggerFactory.getLogger(ServiceFactory.class);
    private static MsgTranslator translator = new MsgTranslator();

    public ServiceFactory(Client client) {
        super(client);
    }

    private static ActorRef createRequestActor(String source, MomClient client, Channel channel, AppMsgWorker requestCB, ActorRef supervisor, boolean cache) {
        ActorRef sup = supervisor;
        if (sup == null) sup = ((Client)client).getMainSupervisor();
        return MomAkkaSupervisor.createNewSupervisedService(
                sup, MsgRequestActor.props(((Client) client), channel, requestCB, cache),
                source + "_msgWorker"
        );
    }

    private static ActorRef createRequestRouter(String source, MomClient client, Channel channel, AppMsgWorker requestCB, ActorRef supervisor, int nbRoutees, boolean cache) {
        Props routeeProps = MsgRequestActor.props(((Client) client), channel, requestCB, cache);
        String routeeNamePrefix = source + "_msgWorker";
        ActorRef sup = supervisor;
        if (sup == null) sup = ((Client)client).getMainSupervisor();
        return MomAkkaSupervisor.createNewSupervisedService(
                sup,
                MomAkkaRequestRouter.props(routeeProps, routeeNamePrefix, nbRoutees),
                source + "_router"
        );
    }

    private static ActorRef createRequestRouter(String source, MomClient client, Channel channel, AppMsgWorker requestCB, ActorRef supervisor, boolean cache) {
        return createRequestRouter(source, client, channel, requestCB, supervisor, ((Client) client).getNbRouteesPerService(), cache);
    }

    private static MomConsumer createConsumer(final String source, final Channel channel, final ActorRef runnableReqActor, final boolean isMsgDebugOnTimeout) {
        return new MomConsumer() {
            private boolean isRunning = false;

            @Override
            public void run() {
                try {
                    Map<String, Object> finalMessage = null;
                    channel.queueDeclare(source, false, false, true, null);

                    QueueingConsumer consumer = new QueueingConsumer(channel);
                    channel.basicConsume(source, false, consumer);
                    isRunning = true;

                    while (isRunning) {
                        try {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery(10);
                            if (delivery!=null && isRunning) {
                                finalMessage = translator.decode(new Message().setEnvelope(delivery.getEnvelope()).
                                        setProperties(delivery.getProperties()).
                                        setBody(delivery.getBody()));
                                if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE) && isMsgDebugOnTimeout)
                                    ((MomLogger)log).setTraceLevel(true);
                                ((MomLogger)log).traceMessage("MomConsumer(" + source + ").run", finalMessage);
                                runnableReqActor.tell(delivery, null);
                                if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE) && isMsgDebugOnTimeout)
                                    ((MomLogger)log).setTraceLevel(false);
                            }
                        } catch (InterruptedException e) {
                            // no message
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    try {
                        if (channel.getConnection()!=null && channel.getConnection().isOpen()) {
                            channel.queueDelete(source);
                            //channel.close();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public boolean isRunning() {
                return isRunning;
            }

            @Override
            public void start() {
                new Thread(this).start();
            }

            @Override
            public void stop() {
                isRunning = false;
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        };
    }

    private static MomMsgGroupServiceMgr createMsgGroupServiceManager(final String source, final Channel channel,
                                                                         final AppMsgWorker requestCB, final MomClient client) {
        return new MomMsgGroupServiceMgr() {
            HashMap<String, MomConsumer> msgGroupConsumersRegistry = new HashMap<>();
            HashMap<String, ActorRef> msgGroupActorRegistry = new HashMap<>();
            @Override
            public void openMsgGroupService(String groupID) {
                final String sessionSource = groupID + "-" + source;
                ActorRef msgGroupSupervisor = ((Client)client).getMsgGroupSupervisor(groupID);
                ActorRef runnableReqActor = null;
                if (msgGroupSupervisor!=null)
                    runnableReqActor = ServiceFactory.createRequestRouter(source, client, channel, requestCB, msgGroupSupervisor, 2, true);
                else {
                    log.warn("No supervisor found for group " + groupID + ". Use main mom supervisor.");
                    runnableReqActor = ServiceFactory.createRequestRouter(sessionSource, client, channel, requestCB, null, 2, true);
                }
                msgGroupActorRegistry.put(groupID, runnableReqActor);
                msgGroupConsumersRegistry.put(groupID, ServiceFactory.createConsumer(sessionSource, channel, runnableReqActor, client.isMsgDebugOnTimeout()));
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
                HashMap<String, MomConsumer> sessionConsumersRegistryClone = (HashMap<String, MomConsumer>) msgGroupConsumersRegistry.clone();
                for (String groupID : sessionConsumersRegistryClone.keySet()) {
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
        final Connection  connection   = ((Client)super.getMomClient()).getConnection();
        MomAkkaService ret = null;
        ActorRef    requestActor ;
        MomConsumer consumer ;
        MomMsgGroupServiceMgr msgGroupMgr ;

        if (connection != null && connection.isOpen()) {
            try {
                Channel channel = connection.createChannel();
                channel.basicQos(1);
                requestActor = ServiceFactory.createRequestRouter(source, super.getMomClient(), channel, requestCB, null, true);
                consumer = ServiceFactory.createConsumer(source, channel, requestActor, super.getMomClient().isMsgDebugOnTimeout());
                msgGroupMgr = ServiceFactory.createMsgGroupServiceManager(source, channel, requestCB, super.getMomClient());
                consumer.start();
                ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient((Client) super.getMomClient()).setMsgGroupServiceMgr(msgGroupMgr);
                super.getServices().add(ret);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return ret;
    }

    /**
     * Create and start a new request service
     *
     * @param source the source where request are coming from
     * @param requestCB the application request worker
     *
     * @return the request service
     *
     */
    @Override
    public MomAkkaService requestService(final String source, final AppMsgWorker requestCB) {
        final Connection  connection   = ((Client)super.getMomClient()).getConnection();
        MomAkkaService ret          = null;
        ActorRef    requestActor ;
        MomConsumer consumer ;

        if (connection != null && connection.isOpen()) {
            try {
                Channel channel = connection.createChannel();
                channel.basicQos(1);
                requestActor = ServiceFactory.createRequestRouter(source, super.getMomClient(), channel, requestCB, null, false);
                consumer = ServiceFactory.createConsumer(source, channel, requestActor, super.getMomClient().isMsgDebugOnTimeout());
                consumer.start();

                ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient(((Client) super.getMomClient()));
                super.getServices().add(ret);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return ret;
    }

    @Override
    public MomAkkaService feederService(String baseDestination, String selector, int interval, AppMsgFeeder feederCB) {
        MomAkkaService ret = null;
        ActorRef feederActor = null;
        Connection  connection   = ((Client)super.getMomClient()).getConnection();
        if (connection != null && connection.isOpen()) {
            ActorRef feeder = ((Client)super.getMomClient()).getActorSystem().actorOf(MsgFeederActor.props(
                    ((Client)super.getMomClient()),baseDestination, selector, feederCB)
            );
            ret = new MomAkkaService().setClient(((Client) super.getMomClient())).setMsgFeeder(feeder, interval);
            super.getServices().add(ret);
        }
        return ret;
    }

    @Override
    public MomAkkaService subscriberService(final String baseSource, String selector, AppMsgWorker feedCB) {
        MomAkkaService ret       = null;
        ActorRef    subsActor = null;
        MomConsumer consumer  = null;
        final Connection connection = ((Client)super.getMomClient()).getConnection();

        if (selector == null || selector.equals(""))
            selector = "#";

        if (connection != null && connection.isOpen()) {
            subsActor = ((Client)super.getMomClient()).getActorSystem().actorOf(
                    MsgSubsActor.props(feedCB), baseSource + "." + ((selector.equals("#")) ? "all" : selector) + "_msgWorker"
            );
            final ActorRef runnableSubsActor = subsActor;
            final String   select           = selector;
            final Client cli = ((Client)super.getMomClient());

            consumer = new MomConsumer() {
                private boolean isRunning = false;

                @Override
                public void run() {
                    Channel channel = null;
                    try {
                        channel = connection.createChannel();
                        channel.exchangeDeclare(baseSource, "topic");

                        String queueName = cli.getClientID()+"_SUBS_2_"+baseSource+"."+select;
                        channel.queueDeclare(queueName, false, true, false, null);
                        channel.queueBind(queueName, baseSource, select);

                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(queueName, true, consumer);

                        isRunning = true;

                        while (isRunning) {
                            try {
                                QueueingConsumer.Delivery delivery = consumer.nextDelivery(10);
                                if (delivery!=null && isRunning) runnableSubsActor.tell(delivery, null);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }
                        if (channel.getConnection().isOpen())
                            channel.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            if (channel.getConnection().isOpen())
                                channel.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                @Override
                public boolean isRunning() {
                    return isRunning;
                }

                @Override
                public void start() {
                    new Thread(this).start();
                }

                @Override
                public void stop() {
                    isRunning = false;
                    try {
                        Thread.sleep(20);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };

            consumer.start();
            ret = new MomAkkaService().setMsgWorker(subsActor).setConsumer(consumer).setClient(
                    ((Client)super.getMomClient())
            );
            super.getServices().add(ret);
        }
        return ret;
    }
}