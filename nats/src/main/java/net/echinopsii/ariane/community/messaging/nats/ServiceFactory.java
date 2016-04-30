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
import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.SyncSubscription;
import net.echinopsii.ariane.community.messaging.api.AppMsgFeeder;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomConsumer;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsServiceFactory;
import net.echinopsii.ariane.community.messaging.common.MomAkkaService;

import java.io.IOException;
import java.util.List;

public class ServiceFactory extends MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {

    public ServiceFactory(Client client) {
        super(client);
    }

    @Override
    public MomAkkaService requestService(final String source, AppMsgWorker requestCB) {
        final Connection connection   = ((Client)super.getMomClient()).getConnection();

        MomAkkaService ret    = null;
        ActorRef requestActor = null;
        MomConsumer consumer  = null;

        if (connection != null && !connection.isClosed()) {
            //connection.publish(Message);
            requestActor = ((Client)super.getMomClient()).getActorSystem().actorOf(
                    MsgRequestActor.props(((Client)super.getMomClient()), requestCB), source + "_msgWorker"
            );
            final ActorRef runnableReqActor   = requestActor;

            consumer = new MomConsumer() {
                private boolean isRunning = false;

                @Override
                public void run() {
                    SyncSubscription subs = null;
                    try {
                        subs = connection.subscribeSync(source);
                        isRunning = true;

                        while (isRunning) {
                            Message msg = subs.nextMessage();
                            runnableReqActor.tell(msg, null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if (subs!=null) {
                            try {
                                subs.unsubscribe();
                                subs.close();
                            } catch (IOException e) {
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
                    new Thread(this).start();
                }

                @Override
                public void stop() {
                    isRunning = false;
                }
            };
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
        ActorRef feederActor = null;
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
        return null;
    }
}
