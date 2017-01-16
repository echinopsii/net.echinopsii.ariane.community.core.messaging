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

import akka.actor.ActorRef;
import akka.actor.Props;
import akka.japi.Creator;
import com.rabbitmq.client.*;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomLogger;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsRequestActor;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * MsgRequestActor class extending {@link net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsRequestActor} abstract class for RabbitMQ MoM
 */
public class MsgRequestActor extends MsgAkkaAbsRequestActor {
    private static final Logger log = MomLoggerFactory.getLogger(MsgRequestActor.class);

    private Channel      channel     = null;

    /**
     * (internal usage only)
     * Return Akka actor Props to spawn a new MsgRequestActor through Akka.
     * Should not be called outside {@link net.echinopsii.ariane.community.messaging.rabbitmq.ServiceFactory#createRequestActor(String, MomClient, Channel, AppMsgWorker, ActorRef, boolean)}
     * or {@link net.echinopsii.ariane.community.messaging.rabbitmq.ServiceFactory#createRequestRouter(String, MomClient, Channel, AppMsgWorker, ActorRef, boolean)}
     *
     * @param mclient the initialized RabbitMQ client
     * @param worker the AppMsgWorker in charge of request treatment
     * @param cache if true will cache last reply in case of retry
     * @return Akka actor Props
     */
    public static Props props(final Client mclient, final Channel channel, final AppMsgWorker worker, final boolean cache) {
        return Props.create(new Creator<MsgRequestActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgRequestActor create() throws Exception {
                return new MsgRequestActor(mclient, channel, worker, cache);
            }
        });
    }

    /**
     * (internal usage only)
     * MsgRequestActor constructor. Should not be called outside {@link this#props}
     *
     * @param mclient the initialized RabbitMQ client
     * @param worker the AppMsgWorker in charge of request treatment
     * @param cache if true will cache last reply in case of retry
     */
    public MsgRequestActor(Client mclient, Channel chan, AppMsgWorker worker, boolean cache) {
        super(mclient, worker, new MsgTranslator(), cache);
        channel = chan;
    }

    /**
     * {@link akka.actor.UntypedActor#onReceive(Object)} implementation.
     * if message instance of {@link com.rabbitmq.client.QueueingConsumer.Delivery} :
     * <br/> decode the message
     * <br/> then request treatment from attached worker and send reply if needed.
     * else unhandled
     * @param message the akka message received by actor
     * @throws IOException if problem encountered while publishing reply or ack request
     */
    @Override
    public void onReceive(Object message) throws IOException {
        if (message instanceof QueueingConsumer.Delivery) {
            Envelope envelope = ((QueueingConsumer.Delivery) message).getEnvelope();
            BasicProperties properties = ((QueueingConsumer.Delivery) message).getProperties();
            byte[] body = ((QueueingConsumer.Delivery)message).getBody();

            Map<String, Object> finalMessage = ((MsgTranslator)super.getTranslator()).decode(new Message().setEnvelope(envelope).
                    setProperties(properties).
                    setBody(body));
            if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) {
                if (super.getClient().isMsgDebugOnTimeout()) ((MomLogger)log).setMsgTraceLevel(true);
                else finalMessage.remove(MomMsgTranslator.MSG_TRACE);
            }
            ((MomLogger)log).traceMessage("MsgRequestActor.onReceive - in", finalMessage);

            Map<String, Object> reply=null;
            if (finalMessage.get(MsgTranslator.MSG_CORRELATION_ID)!=null &&
                    super.getReplyFromCache((String) finalMessage.get(MsgTranslator.MSG_CORRELATION_ID))!=null)
                reply = super.getReplyFromCache((String) finalMessage.get(MsgTranslator.MSG_CORRELATION_ID));
            if (reply==null) reply = super.getMsgWorker().apply(finalMessage);
            else log.debug("reply from cache !");

            if (finalMessage.get(MsgTranslator.MSG_CORRELATION_ID)!=null)
                super.putReplyToCache((String) finalMessage.get(MsgTranslator.MSG_CORRELATION_ID),reply);

            if (properties.getReplyTo()!=null && properties.getCorrelationId()!=null && reply!=null) {
                reply.put(MsgTranslator.MSG_CORRELATION_ID, properties.getCorrelationId());
                if (super.getClient().getClientID()!=null)
                    reply.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
                Message replyMessage = ((MsgTranslator)super.getTranslator()).encode(reply);
                String replyTo = properties.getReplyTo();
                channel.basicPublish("", replyTo, (AMQP.BasicProperties) replyMessage.getProperties(), replyMessage.getBody());
            }
            channel.basicAck(((QueueingConsumer.Delivery)message).getEnvelope().getDeliveryTag(), false);

            ((MomLogger)log).traceMessage("MsgRequestActor.onReceive - out", finalMessage);
            if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setMsgTraceLevel(false);
        } else
            unhandled(message);
    }
}