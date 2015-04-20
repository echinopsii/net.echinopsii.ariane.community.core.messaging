/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
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
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.QueueingConsumer;
import net.echinopsii.ariane.community.messaging.api.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ServiceFactory implements MomServiceFactory<Service, AppMsgWorker, AppMsgFeeder, String> {

    private Client        momClient ;
    private List<Service> serviceList  = new ArrayList<Service>();

    public ServiceFactory(Client client) {
        momClient = client;
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
    public Service requestService(final String source, final AppMsgWorker requestCB) {
        final Connection  connection   = momClient.getConnection();
        Service     ret          = null;
        ActorRef    requestActor = null;
        MomConsumer consumer     = null;

        if (connection != null && connection.isOpen()) {
            Channel channel = null;
            try {
                channel = connection.createChannel();
                channel.basicQos(1);
            } catch (IOException e) {
                e.printStackTrace();
            }

            requestActor = momClient.getActorSystem().actorOf(MsgRequestActor.props(momClient, channel, requestCB), source + "_msgWorker");
            final ActorRef runnableReqActor   = requestActor;
            final Channel  runnableChannel    = channel;

            consumer = new MomConsumer() {
                private boolean isRunning = false;

                @Override
                public void run() {
                    try {
                        runnableChannel.queueDeclare(source, false, false, false, null);

                        QueueingConsumer consumer = new QueueingConsumer(runnableChannel);
                        runnableChannel.basicConsume(source, false, consumer);
                        isRunning = true;

                        while (isRunning) {
                            try {
                                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                runnableReqActor.tell(delivery, null);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
                            runnableChannel.close();
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
                }
            };
            consumer.start();

            ret = new Service().setMsgWorker(requestActor).setConsumer(consumer).setClient(momClient);
            serviceList.add(ret);
        }

        return ret;
    }

    @Override
    public Service feederService(String baseDestination, String selector, int interval, AppMsgFeeder feederCB) {
        Service  ret = null;
        ActorRef feederActor = null;
        Connection  connection   = momClient.getConnection();
        if (connection != null && connection.isOpen()) {
            ActorRef feeder = momClient.getActorSystem().actorOf(MsgFeederActor.props(momClient,baseDestination, selector, feederCB));
            ret = new Service().setClient(momClient).setMsgFeeder(feeder, interval);
            serviceList.add(ret);
        }
        return ret;
    }

    @Override
    public Service subscriberService(final String baseSource, String selector, AppMsgWorker feedCB) {
        Service     ret       = null;
        ActorRef    subsActor = null;
        MomConsumer consumer  = null;
        final Connection connection = momClient.getConnection();

        if (selector == null || selector.equals(""))
            selector = "#";

        if (connection != null && connection.isOpen()) {
            subsActor = momClient.getActorSystem().actorOf(MsgSubsActor.props(feedCB), baseSource + "." + ((selector.equals("#")) ? "all" : selector) + "_msgWorker");
            final ActorRef runnableReqActor = subsActor;
            final String   select           = selector;

            consumer = new MomConsumer() {
                private boolean isRunning = false;

                @Override
                public void run() {
                    Channel channel = null;
                    try {
                        channel = connection.createChannel();
                        channel.exchangeDeclare(baseSource, "topic");

                        String queueName = momClient.getClientID()+"_SUBS_2_"+baseSource+"."+select;
                        channel.queueDeclare(queueName, false, true, false, null);
                        channel.queueBind(queueName, baseSource, select);

                        QueueingConsumer consumer = new QueueingConsumer(channel);
                        channel.basicConsume(queueName, true, consumer);

                        isRunning = true;

                        while (isRunning) {
                            try {
                                QueueingConsumer.Delivery delivery = consumer.nextDelivery();
                                runnableReqActor.tell(delivery, null);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }

                        }

                        channel.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        try {
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
                }
            };

            consumer.start();
            ret = new Service().setMsgWorker(subsActor).setConsumer(consumer).setClient(momClient);
            serviceList.add(ret);
        }
        return ret;
    }

    @Override
    public List<Service> getServices() {
        return serviceList;
    }
}