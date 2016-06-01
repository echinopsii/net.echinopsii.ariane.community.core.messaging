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

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.nats.client.Message;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.nats.tools.PropertiesException;
import net.echinopsii.ariane.community.messaging.nats.tools.PropertiesJSON;
import net.echinopsii.ariane.community.messaging.nats.tools.ToolBox;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MsgTranslator implements MomMsgTranslator<Message> {

    public final static String MSG_NATS_SUBJECT = "MSG_NATS_SUBJECT";

    class ExtendedNATSMessage {
        HashMap<String, Object> properties = new HashMap<>();
        byte[] body ;

        public HashMap<String, Object> getProperties() {
            return properties;
        }

        public void setProperties(HashMap<String, Object> properties) {
            this.properties = properties;
        }

        public byte[] getBody() {
            return body;
        }

        public void setBody(byte[] body) {
            this.body = body;
        }

        public byte[] toBSON() throws IOException {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            JsonGenerator jgenerator = ToolBox.jFactory.createJsonGenerator(outStream, JsonEncoding.UTF8);
            jgenerator.writeStartObject();
            //jgenerator.writeObjectFieldStart("properties");
            //PropertiesJSON.propertiesToJSON(properties, jgenerator);
            //jgenerator.writeEndObject();
            jgenerator.writeArrayFieldStart("properties");
            for (PropertiesJSON.TypedPropertyField field : PropertiesJSON.propertiesToTypedPropertiesList(properties))
                field.toJSON(jgenerator);
            jgenerator.writeEndArray();
            if (this.body!=null)jgenerator.writeBinaryField("body", this.body);
            jgenerator.writeEndObject();
            jgenerator.close();
            String result = ToolBox.getOuputStreamContent(outStream, "UTF-8");
            return result.getBytes();
        }
    }

    static class JSONDeserializedExtendedNATSMessage {
        List<PropertiesJSON.TypedPropertyField> properties;
        byte[] body ;

        public JSONDeserializedExtendedNATSMessage() {
        }

        public byte[] getBody() {
            return body;
        }

        public void setBody(byte[] body) {
            this.body = body;
        }

        public List<PropertiesJSON.TypedPropertyField> getProperties() {
            return properties;
        }

        public void setProperties(List<PropertiesJSON.TypedPropertyField> properties) {
            this.properties = properties;
        }
    }

    public ExtendedNATSMessage fromJSON(byte[] payload) throws IOException, PropertiesException {
        ExtendedNATSMessage extendedNATSMessage = new ExtendedNATSMessage();
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String payload_s = new String((byte [])payload);//.split("\n")[0];
        JSONDeserializedExtendedNATSMessage jsonDeserializedExtendedNATSMessage = mapper.readValue(payload_s, JSONDeserializedExtendedNATSMessage.class);
        extendedNATSMessage.setBody(jsonDeserializedExtendedNATSMessage.getBody());
        for (PropertiesJSON.TypedPropertyField deserializedProperty : jsonDeserializedExtendedNATSMessage.getProperties()) {
            Object oValue = ToolBox.extractPropertyObjectValueFromString(deserializedProperty.getPropertyValue(), deserializedProperty.getPropertyType());
            extendedNATSMessage.getProperties().put(deserializedProperty.getPropertyName(), oValue);
        }
        return extendedNATSMessage;
    }

    @Override
    public Map<String, Class> getMessageTypo() {
        return null;
    }

    @Override
    public Message encode(Map<String, Object> message) {
        Message ret = new Message();
        ExtendedNATSMessage extendedNATSMessage = new ExtendedNATSMessage();

        for (String key : message.keySet()) {
            if (key.equals(MSG_REPLY_TO)) {
                ret.setReplyTo((String)message.get(MSG_REPLY_TO));
            } else if (key.equals(MSG_NATS_SUBJECT)) {
                ret.setSubject((String) message.get(MSG_NATS_SUBJECT));
            } else if (key.equals(MSG_BODY)) {
                Object bodyObject = message.get(MSG_BODY);
                if (bodyObject instanceof String)
                    extendedNATSMessage.setBody(((String) message.get(MSG_BODY)).getBytes());
                else if (bodyObject instanceof byte[])
                    extendedNATSMessage.setBody((byte []) bodyObject);
            } else extendedNATSMessage.getProperties().put(key, message.get(key));
        }

        byte[] data = null;
        try {
            data = extendedNATSMessage.toBSON();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (data!=null) ret.setData(data);

        return ret;
    }

    @Override
    public Map<String, Object> decode(Message message) {
        LinkedHashMap<String, Object> decodedMessage = new LinkedHashMap();
        ExtendedNATSMessage extendedNATSMessage = null;
        try {
            extendedNATSMessage = this.fromJSON(message.getData());
            decodedMessage.put(MSG_REPLY_TO, message.getReplyTo());
            decodedMessage.put(MSG_NATS_SUBJECT, message.getSubject());
            decodedMessage.putAll(extendedNATSMessage.getProperties());
            decodedMessage.put(MSG_BODY, extendedNATSMessage.getBody());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return decodedMessage;
    }
}
