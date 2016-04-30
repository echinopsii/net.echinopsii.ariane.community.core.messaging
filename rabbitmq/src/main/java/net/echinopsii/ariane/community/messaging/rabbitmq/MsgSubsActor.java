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

public class MsgSubsActor extends MsgAkkaAbsSubsActor {

    public static Props props(final AppMsgWorker worker) {
        return Props.create(new Creator<MsgSubsActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgSubsActor create() throws Exception {
                return new MsgSubsActor(worker);
            }
        });
    }

    public MsgSubsActor(AppMsgWorker worker) {
        super(worker, new MsgTranslator());
    }

    @Override
    public void onReceive(Object message) throws Exception {
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