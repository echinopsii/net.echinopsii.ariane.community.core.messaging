/**
 * Messaging - RabbitMQ Implementation
 * Message Translator implementation
 * Copyright (C) 8/25/14 echinopsii
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

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.Envelope;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class MsgTranslator implements MomMsgTranslator<Message>{

    public final static String MSG_RBQ_DELIVERY_TAG     = "MSG_RBQ_DELIVERY_TAG";
    public final static String MSG_RBQ_EXCHANGE         = "MSG_RBQ_EXCHANGE";
    public final static String MSG_RBQ_ROUTINGKEY       = "MSG_RBQ_ROUTINGKEY";
    public final static String MSG_RBQ_HEADER           = "MSG_RBQ_HEADER";
    public final static String MSG_RBQ_CONTENT_ENCODING = "MSG_RBQ_CONTENT_ENCODING";
    public final static String MSG_RBQ_CONTENT_TYPE     = "MSG_RBQ_CONTENT_TYPE";
    public final static String MSG_RBQ_USER_ID          = "MSG_RBQ_USER_ID";

    @Override
    public Map<String, Class> getMessageTypo() {
        return null;
    }

    @Override
    public Message encode(Map<String, Object> message) {
        AMQP.BasicProperties.Builder propsBuilder = new AMQP.BasicProperties.Builder();
        Map<String,Object> headerFields = new HashMap<String, Object>();
        BasicProperties properties = null;
        byte[] body = null;

        for (String key : message.keySet()) {
            if (key.equals(MSG_APPLICATION_ID))
                propsBuilder.appId((String) message.get(MSG_APPLICATION_ID));
            else if (key.equals(MSG_RBQ_CONTENT_ENCODING))
                propsBuilder.contentEncoding((String) message.get(MSG_RBQ_CONTENT_ENCODING));
            else if (key.equals(MSG_RBQ_CONTENT_TYPE))
                propsBuilder.contentType((String) message.get(MSG_RBQ_CONTENT_TYPE));
            else if (key.equals(MSG_CORRELATION_ID))
                propsBuilder.correlationId((String) message.get(MSG_CORRELATION_ID));
            else if (key.equals(MSG_DELIVERY_MODE))
                propsBuilder.deliveryMode((Integer) message.get(MSG_DELIVERY_MODE));
            else if (key.equals(MSG_EXPIRATION))
                propsBuilder.expiration((String)message.get(MSG_EXPIRATION));
            else if (key.equals(MSG_RBQ_HEADER))
                headerFields.putAll((Map<String, Object>)message.get(MSG_RBQ_HEADER));
            else if (key.equals(MSG_PRIORITY))
                propsBuilder.priority((Integer)message.get(MSG_PRIORITY));
            else if (key.equals(MSG_REPLY_TO))
                propsBuilder.replyTo((String)message.get(MSG_REPLY_TO));
            else if (key.equals(MSG_TIMESTAMP))
                propsBuilder.timestamp((Date)message.get(MSG_TIMESTAMP));
            else if (key.equals(MSG_TYPE))
                propsBuilder.type((String)message.get(MSG_TYPE));
            else if (key.equals(MSG_RBQ_USER_ID))
                propsBuilder.userId((String)message.get(MSG_RBQ_USER_ID));
            else if (key.equals(MSG_BODY)) {
                Object bodyObject = message.get(MSG_BODY);
                if (bodyObject instanceof String)
                    body = ((String) message.get(MSG_BODY)).getBytes();
                else if (bodyObject instanceof byte[])
                    body = (byte[]) bodyObject;
            } else {
                headerFields.put(key, message.get(key));
            }
        }

        if (headerFields.size()>0)
            propsBuilder.headers(headerFields);
        properties = propsBuilder.build();

        return new Message().setProperties(properties).setBody(body);
    }

    @Override
    public Map<String, Object> decode(Message message) {
        Envelope envelope = message.getEnvelope();
        BasicProperties properties = (BasicProperties) message.getProperties();
        byte[] body = message.getBody();

        LinkedHashMap<String, Object> decodedMessage = new LinkedHashMap<String, Object>();
        if (envelope!=null) {
            decodedMessage.put(MSG_RBQ_DELIVERY_TAG, envelope.getDeliveryTag());
            decodedMessage.put(MSG_RBQ_EXCHANGE, envelope.getExchange());
            decodedMessage.put(MSG_RBQ_ROUTINGKEY, envelope.getRoutingKey());
        }

        if (properties!=null) {
            decodedMessage.put(MSG_APPLICATION_ID, properties.getAppId());
            decodedMessage.put(MSG_RBQ_CONTENT_ENCODING, properties.getContentEncoding());
            decodedMessage.put(MSG_RBQ_CONTENT_TYPE, properties.getContentType());
            decodedMessage.put(MSG_CORRELATION_ID, properties.getCorrelationId());
            decodedMessage.put(MSG_DELIVERY_MODE, properties.getDeliveryMode());
            decodedMessage.put(MSG_EXPIRATION, properties.getExpiration());
            decodedMessage.put(MSG_MESSAGE_ID, properties.getMessageId());
            decodedMessage.put(MSG_PRIORITY, properties.getPriority());
            decodedMessage.put(MSG_REPLY_TO, properties.getReplyTo());
            decodedMessage.put(MSG_TIMESTAMP, properties.getTimestamp());
            decodedMessage.put(MSG_TYPE, properties.getType());
            decodedMessage.put(MSG_RBQ_USER_ID, properties.getUserId());
            Map<String, Object> headerFields = properties.getHeaders();
            if (headerFields!=null) {
                for (String key : headerFields.keySet())
                    decodedMessage.put(key, headerFields.get(key));
            }
        }

        decodedMessage.put(MSG_BODY, body);

        return decodedMessage;
    }
}