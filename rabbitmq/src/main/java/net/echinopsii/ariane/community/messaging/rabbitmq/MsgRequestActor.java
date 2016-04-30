/**
 * Messaging - RabbitMQ Implementation
 * Message Request Actor
 * Copyright (C) 28/08/14 echinopsii
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
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.rabbitmq.client.*;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsRequestActor;

import java.util.Map;

public class MsgRequestActor extends MsgAkkaAbsRequestActor {

    private Channel      channel     = null;

    public static Props props(final Client mclient, final Channel channel, final AppMsgWorker worker) {
        return Props.create(new Creator<MsgRequestActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgRequestActor create() throws Exception {
                return new MsgRequestActor(mclient, channel, worker);
            }
        });
    }

    public MsgRequestActor(Client mclient, Channel chan, AppMsgWorker worker) {
        super(mclient, worker, new MsgTranslator());
        channel = chan;
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof QueueingConsumer.Delivery) {
            Envelope envelope = ((QueueingConsumer.Delivery) message).getEnvelope();
            BasicProperties properties = ((QueueingConsumer.Delivery) message).getProperties();
            byte[] body = ((QueueingConsumer.Delivery)message).getBody();

            Map<String, Object> finalMessage = ((MsgTranslator)super.getTranslator()).decode(new Message().setEnvelope(envelope).
                                                                 setProperties(properties).
                                                                 setBody(body));

            Map<String, Object> reply = super.getMsgWorker().apply(finalMessage);
            if (properties.getReplyTo()!=null && properties.getCorrelationId()!=null && reply!=null) {
                reply.put(MsgTranslator.MSG_CORRELATION_ID, properties.getCorrelationId());
                reply.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
                Message replyMessage = ((MsgTranslator)super.getTranslator()).encode(reply);
                String replyTo = properties.getReplyTo();
                channel.basicPublish("", replyTo, (AMQP.BasicProperties) replyMessage.getProperties(), replyMessage.getBody());
            }
            channel.basicAck(((QueueingConsumer.Delivery)message).getEnvelope().getDeliveryTag(), false);
        } else
            unhandled(message);
    }
}