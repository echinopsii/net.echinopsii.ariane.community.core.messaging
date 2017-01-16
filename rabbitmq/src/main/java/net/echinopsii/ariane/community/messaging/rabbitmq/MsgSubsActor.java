/**
 * Messaging - RabbitMQ Implementation
 * Message Subscriber Actor
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

import akka.actor.Props;
import akka.japi.Creator;
import com.rabbitmq.client.*;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsSubsActor;

import java.util.Map;

/**
 * MsgSubsActor class extending {@link net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsSubsActor} abstract class for RabbitMQ MoM
 */
public class MsgSubsActor extends MsgAkkaAbsSubsActor {

    /**
     * (internal usage only)
     * Return Akka actor Props to spawn a new MsgRequestActor through Akka.
     * Should not be called outside {@link net.echinopsii.ariane.community.messaging.rabbitmq.ServiceFactory#subscriberService(String, String, AppMsgWorker)}
     *
     * @param worker the AppMsgWorker in charge of subscription message feed treatment
     * @return Akka actor Props
     */
    public static Props props(final AppMsgWorker worker) {
        return Props.create(new Creator<MsgSubsActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgSubsActor create() throws Exception {
                return new MsgSubsActor(worker);
            }
        });
    }

    /**
     * (internal usage only)
     * MsgSubsActor constructor. Should not be called outside {@link this#props}
     *
     * @param worker the AppMsgWorker in charge of subscription message feed treatment
     */
    public MsgSubsActor(AppMsgWorker worker) {
        super(worker, new MsgTranslator());
    }

    /**
     * {@link akka.actor.UntypedActor#onReceive(Object)} implementation.
     * if message instance of {@link com.rabbitmq.client.QueueingConsumer.Delivery} decode the message then request treatment from attached worker.
     * else unhandled
     * @param message the akka message received by actor
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof QueueingConsumer.Delivery) {
            Envelope envelope = ((QueueingConsumer.Delivery) message).getEnvelope();
            BasicProperties properties = ((QueueingConsumer.Delivery) message).getProperties();
            byte[] body = ((QueueingConsumer.Delivery)message).getBody();

            Map<String, Object> finalMessage = ((MsgTranslator)super.getTranslator()).decode(
                                                   new Message().setEnvelope(((QueueingConsumer.Delivery) message).getEnvelope()).
                                                                 setProperties(((QueueingConsumer.Delivery) message).getProperties()).
                                                                 setBody(((QueueingConsumer.Delivery) message).getBody()));

            super.getMsgWorker().apply(finalMessage);
        } else
            unhandled(message);
    }
}