/**
 * Messaging - NATS Implementation
 * Message Translator implementation
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

import io.nats.client.Message;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;

import java.util.LinkedHashMap;
import java.util.Map;

public class MsgTranslator implements MomMsgTranslator<Message> {

    public final static String MSG_NATS_SUBJECT = "MSG_NATS_SUBJECT";

    @Override
    public Map<String, Class> getMessageTypo() {
        return null;
    }

    @Override
    public Message encode(Map<String, Object> message) {
        Message ret = new Message();
        for (String key : message.keySet()) {
            if (key.equals(MSG_REPLY_TO)) {
                ret.setReplyTo((String)message.get(MSG_REPLY_TO));
            } else if (key.equals(MSG_NATS_SUBJECT)) {
                ret.setSubject((String) message.get(MSG_NATS_SUBJECT));
            } else if (key.equals(MSG_BODY)) {
                Object bodyObject = message.get(MSG_BODY);
                if (bodyObject instanceof String)
                    ret.setData(((String) message.get(MSG_BODY)).getBytes());
                else if (bodyObject instanceof byte[])
                    ret.setData((byte[]) bodyObject);
            }
        }
        return ret;
    }

    @Override
    public Map<String, Object> decode(Message message) {
        LinkedHashMap<String, Object> decodedMessage = new LinkedHashMap<String, Object>();
        decodedMessage.put(MSG_REPLY_TO, message.getReplyTo());
        decodedMessage.put(MSG_NATS_SUBJECT, message.getSubject());
        decodedMessage.put(MSG_BODY, message.getData());
        return decodedMessage;
    }
}
