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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import net.echinopsii.ariane.community.messaging.api.*;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsServiceFactory;
import net.echinopsii.ariane.community.messaging.common.MomAkkaService;

import java.io.IOException;
import java.util.HashMap;

public class ServiceFactory extends MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {

    public ServiceFactory(Client client) {
        super(client);
    }

    private static ActorRef createRequestActor(String source, MomClient client, Channel channel, AppMsgWorker requestCB) {
        return ((Client)client).getActorSystem().actorOf(
                MsgRequestActor.props(((Client)client), channel, requestCB), source + "_msgWorker"
        );
    }

    private static MomConsumer createConsumer(final String source, final Channel channel, final ActorRef runnableReqActor) {
        return new MomConsumer() {
            private boolean isRunning = false;

            @Override
            public void run() {
                try {
                    channel.queueDeclare(source, false, false, true, null);

                    QueueingConsumer consumer = new QueueingConsumer(channel);
                    channel.basicConsume(source, false, consumer);
                    isRunning = true;

                    while (isRunning) {
                        try {
                            QueueingConsumer.Delivery delivery = consumer.nextDelivery(10);
                            if (delivery!=null && isRunning) runnableReqActor.tell(delivery, null);
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

    private static MomMsgGroupSubServiceMgr createSessionManager(final String source, final Channel channel,
                                                                 final AppMsgWorker requestCB, final MomClient client) {
        return new MomMsgGroupSubServiceMgr() {
            HashMap<String, MomConsumer> sessionConsumersRegistry = new HashMap<>();
            HashMap<String, ActorRef> sessionActorRegistry = new HashMap<>();
            @Override
            public void openMsgGroupSubService(String groupID) {
                final String sessionSource = groupID + "-" + source;
                ActorRef runnableReqActor = ServiceFactory.createRequestActor(sessionSource, client, channel, requestCB);
                sessionActorRegistry.put(groupID, runnableReqActor);
                sessionConsumersRegistry.put(groupID, ServiceFactory.createConsumer(sessionSource, channel, runnableReqActor));
                sessionConsumersRegistry.get(groupID).start();
            }

            @Override
            public void closeMsgGroupSubService(String groupID) {
                if (sessionConsumersRegistry.containsKey(groupID)) {
                    sessionConsumersRegistry.get(groupID).stop();
                    sessionConsumersRegistry.remove(groupID);
                    ((Client)client).getActorSystem().stop(sessionActorRegistry.get(groupID));
                    if (!sessionActorRegistry.get(groupID).isTerminated()) sessionActorRegistry.get(groupID).tell(PoisonPill.getInstance(), null);
                    sessionActorRegistry.remove(groupID);
                }
            }

            @Override
            public void stop() {
                HashMap<String, MomConsumer> sessionConsumersRegistryClone = (HashMap<String, MomConsumer>) sessionConsumersRegistry.clone();
                for (String sessionID : sessionConsumersRegistryClone.keySet()) {
                    sessionConsumersRegistry.get(sessionID).stop();
                    sessionConsumersRegistry.remove(sessionID);
                    ((Client)client).getActorSystem().stop(sessionActorRegistry.get(sessionID));
                    if (!sessionActorRegistry.get(sessionID).isTerminated()) sessionActorRegistry.get(sessionID).tell(PoisonPill.getInstance(), null);
                    sessionActorRegistry.remove(sessionID);
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
        MomMsgGroupSubServiceMgr msgGroupMgr ;

        if (connection != null && connection.isOpen()) {
            try {
                Channel channel = connection.createChannel();
                channel.basicQos(1);
                requestActor = ServiceFactory.createRequestActor(source, super.getMomClient(), channel, requestCB);
                consumer = ServiceFactory.createConsumer(source, channel, requestActor);
                msgGroupMgr = ServiceFactory.createSessionManager(source, channel, requestCB, super.getMomClient());
                consumer.start();
                ret = new MomAkkaService().setMsgWorker(requestActor).setConsumer(consumer).setClient((Client) super.getMomClient()).setMsgGroupSubServiceMgr(msgGroupMgr);
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
                requestActor = ServiceFactory.createRequestActor(source, super.getMomClient(), channel, requestCB);
                consumer = ServiceFactory.createConsumer(source, channel, requestActor);
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