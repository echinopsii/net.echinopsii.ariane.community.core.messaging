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
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.nats.tools.PropertiesException;
import net.echinopsii.ariane.community.messaging.nats.tools.PropertiesJSON;
import net.echinopsii.ariane.community.messaging.nats.tools.ToolBox;
import org.slf4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;

public class MsgTranslator implements MomMsgTranslator<Message[]> {
    private static final Logger log = MomLoggerFactory.getLogger(MsgTranslator.class);

    public final static String MSG_NATS_SUBJECT = "MSG_NATS_SUBJECT";
    public static long MSG_MAX_SIZE = 0;

    public static void setMsgMaxSize(long maxPayload) {
        MSG_MAX_SIZE = maxPayload;
    }



    private static int getBSONMsgPayloadSize(Map<String, Object> msg) {
        int ret = 0;
        HashMap<String, Object> propMap = new HashMap<>(msg);
        propMap.remove(MSG_BODY);
        propMap.remove(MSG_NATS_SUBJECT);
        propMap.remove(MSG_REPLY_TO);

        Object bodyObject = msg.get(MSG_BODY);
        byte[] body = null;

        if (bodyObject!=null) {
            if (bodyObject instanceof String)
                body = ((String) msg.get(MSG_BODY)).getBytes();
            else if (bodyObject instanceof byte[])
                body = (byte[]) bodyObject;
        }

        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonGenerator jgenerator = null;
        try {
            jgenerator = ToolBox.jFactory.createJsonGenerator(outStream, JsonEncoding.UTF8);
            jgenerator.writeStartObject();
            jgenerator.writeArrayFieldStart("properties");
            for (PropertiesJSON.TypedPropertyField field : PropertiesJSON.propertiesToTypedPropertiesList(propMap))
                field.toJSON(jgenerator);
            jgenerator.writeEndArray();
            if (body!=null) jgenerator.writeBinaryField("body", body);
            jgenerator.writeEndObject();
            jgenerator.close();
            ret = ToolBox.getOuputStreamContent(outStream, "UTF-8").getBytes().length;
        } catch (IOException e) {
            e.printStackTrace();
        } catch (PropertiesException e) {
            e.printStackTrace();
        }
        return ret;
    }

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

        public byte[] toBSON() {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            String result = "";
            JsonGenerator jgenerator = null;
            try {
                jgenerator = ToolBox.jFactory.createJsonGenerator(outStream, JsonEncoding.UTF8);
                jgenerator.writeStartObject();
                jgenerator.writeArrayFieldStart("properties");
                for (PropertiesJSON.TypedPropertyField field : PropertiesJSON.propertiesToTypedPropertiesList(properties))
                    field.toJSON(jgenerator);
                jgenerator.writeEndArray();
                if (this.body!=null)jgenerator.writeBinaryField("body", this.body);
                jgenerator.writeEndObject();
                jgenerator.close();
                result = ToolBox.getOuputStreamContent(outStream, "UTF-8");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (PropertiesException e) {
                e.printStackTrace();
            }
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
    public Message[] encode(Map<String, Object> message) {
        Message[] ret = null;

        int bsonMsgPayloadSize =  ((message.get(MSG_REPLY_TO)!=null) ? MSG_REPLY_TO.getBytes().length + message.get(MSG_REPLY_TO).toString().getBytes().length : 0) +
                                  ((message.get(MSG_NATS_SUBJECT)!=null) ? MSG_NATS_SUBJECT.getBytes().length + message.get(MSG_NATS_SUBJECT).toString().getBytes().length : 0);;
        int natsPropsSize = getBSONMsgPayloadSize(message);

        if (bsonMsgPayloadSize < (MSG_MAX_SIZE - natsPropsSize)) {
            Message finalMessage = new Message();
            ExtendedNATSMessage extendedNATSMessage = new ExtendedNATSMessage();
            for (String key : message.keySet()) {
                switch (key) {
                    case MSG_REPLY_TO:
                        finalMessage.setReplyTo((String) message.get(MSG_REPLY_TO));
                        break;
                    case MSG_NATS_SUBJECT:
                        finalMessage.setSubject((String) message.get(MSG_NATS_SUBJECT));
                        break;
                    case MSG_BODY:
                        Object bodyObject = message.get(MSG_BODY);
                        if (bodyObject instanceof String)
                            extendedNATSMessage.setBody(((String) message.get(MSG_BODY)).getBytes());
                        else if (bodyObject instanceof byte[])
                            extendedNATSMessage.setBody((byte[]) bodyObject);
                        break;
                    default:
                        extendedNATSMessage.getProperties().put(key, message.get(key));
                        break;
                }
            }

            byte[] data = extendedNATSMessage.toBSON();
            finalMessage.setData(data);
            ret = new Message[]{finalMessage};
        } else {
            String splitMID = null;
            synchronized (UUID.class) {
                splitMID = UUID.randomUUID().toString();
            }

            HashMap<String, Object> wipMsgField = new HashMap<>(message);
            wipMsgField.remove(MSG_BODY);
            wipMsgField.remove(MSG_NATS_SUBJECT);
            wipMsgField.remove(MSG_REPLY_TO);
            wipMsgField.remove(MSG_TRACE);

            int consumedBodyOffset = 0;
            byte[] wipBody = null;
            int wipBodyLength = 0;
            Object bodyObject = message.get(MSG_BODY);
            if (bodyObject != null && bodyObject instanceof String) wipBody = ((String) message.get(MSG_BODY)).getBytes();
            else if (bodyObject != null && bodyObject instanceof byte[]) wipBody = (byte[]) bodyObject;
            if (wipBody!=null) wipBodyLength = wipBody.length;

            ArrayList<ExtendedNATSMessage> splittedENATSMsg = new ArrayList<>();
            int splitOID = 0;
            while((wipBodyLength - consumedBodyOffset)>0 || wipMsgField.size()>0) {
                int wipENATSMsgLength = 0;
                ExtendedNATSMessage wipENATSMsg = new ExtendedNATSMessage();
                wipENATSMsg.getProperties().put(MSG_SPLIT_MID, splitMID);
                wipENATSMsg.getProperties().put(MSG_SPLIT_COUNT, Integer.MAX_VALUE); //TO BE REDEFINE
                wipENATSMsg.getProperties().put(MSG_SPLIT_OID, splitOID);
                if (message.get(MSG_TRACE)!=null) wipENATSMsg.getProperties().put(MSG_TRACE, message.get(MSG_TRACE));

                // push properties first
                for (String key: message.keySet()) {
                    if (wipMsgField.containsKey(key)) {
                        wipENATSMsg.getProperties().put(key, message.get(key));
                        wipENATSMsgLength = wipENATSMsg.toBSON().length;
                        if (wipENATSMsgLength < MSG_MAX_SIZE)
                            wipMsgField.remove(key);
                        else wipENATSMsg.getProperties().remove(key);
                    }
                }

                // if some place left on wipENATSMsg push some chunk from body
                if (wipBodyLength > 0) {
                    wipENATSMsgLength = wipENATSMsg.toBSON().length;

                    int reduction = 0;
                    int chunkSize = (int) (MSG_MAX_SIZE - wipENATSMsgLength);
                    if (consumedBodyOffset + chunkSize > wipBodyLength) chunkSize = wipBodyLength-consumedBodyOffset;
                    byte[] chunkBody = new byte[chunkSize];

                    int bodyIdx = 0;
                    for (int b = consumedBodyOffset; b<(consumedBodyOffset+chunkSize); b++) {
                        chunkBody[bodyIdx] = wipBody[b];
                        bodyIdx++;
                    }
                    wipENATSMsg.setBody(chunkBody);
                    wipENATSMsgLength = wipENATSMsg.toBSON().length;

                    while (wipENATSMsgLength > MSG_MAX_SIZE) {
                        ++reduction;
                        chunkSize -= chunkSize*((double)reduction/(double)10);
                        if (consumedBodyOffset + chunkSize > wipBodyLength) chunkSize = wipBodyLength-consumedBodyOffset;
                        chunkBody = new byte[chunkSize];
                        bodyIdx = 0;
                        for (int b = consumedBodyOffset; b<(consumedBodyOffset+chunkSize); b++) {
                            chunkBody[bodyIdx] = wipBody[b];
                            bodyIdx++;
                        }
                        wipENATSMsg.setBody(chunkBody);
                        wipENATSMsgLength = wipENATSMsg.toBSON().length;
                    }

                    consumedBodyOffset+=bodyIdx;
                }

                splittedENATSMsg.add(splitOID, wipENATSMsg);
                splitOID++;
            }

            int splitCount = splittedENATSMsg.size();
            ret = new Message[splitCount];
            for (int i=0; i<splittedENATSMsg.size(); i++) {
                Message splittedMessage = new Message();
                if (message.get(MSG_NATS_SUBJECT)!=null) splittedMessage.setSubject((String) message.get(MSG_NATS_SUBJECT));
                if (message.get(MSG_REPLY_TO)!=null) splittedMessage.setReplyTo((String) message.get(MSG_REPLY_TO));

                ExtendedNATSMessage extendedNATSMessage = splittedENATSMsg.get(i);
                extendedNATSMessage.getProperties().put(MSG_SPLIT_COUNT, splitCount);

                byte[] data = extendedNATSMessage.toBSON();
                splittedMessage.setData(data);
                ret[i] = splittedMessage;
            }
        }

        return ret;
    }

    @Override
    public Map<String, Object> decode(Message[] message) {
        LinkedHashMap<String, Object> decodedMessage = new LinkedHashMap();
        byte[][] bodyChunks = null;

        boolean initDone = false;

        for (Message messagePart : message) {
            try {
                ExtendedNATSMessage extendedNATSMessage = this.fromJSON(messagePart.getData());
                if (!initDone) {
                    decodedMessage.put(MSG_REPLY_TO, messagePart.getReplyTo());
                    decodedMessage.put(MSG_NATS_SUBJECT, messagePart.getSubject());
                    decodedMessage.putAll(extendedNATSMessage.getProperties());
                    if (extendedNATSMessage.getProperties().get(MSG_SPLIT_COUNT) == null || extendedNATSMessage.getProperties().get(MSG_SPLIT_COUNT) == 1)
                        decodedMessage.put(MSG_BODY, extendedNATSMessage.getBody());
                    else if (((int)extendedNATSMessage.getProperties().get(MSG_SPLIT_COUNT)) == message.length && extendedNATSMessage.getBody() != null) {
                        if (bodyChunks==null) bodyChunks = new byte[(int)extendedNATSMessage.getProperties().get(MSG_SPLIT_COUNT)][];
                        bodyChunks[(int) extendedNATSMessage.getProperties().get(MSG_SPLIT_OID)] = extendedNATSMessage.getBody();
                    }
                    initDone = true;
                } else {
                    decodedMessage.putAll(extendedNATSMessage.getProperties());
                    if (bodyChunks==null) bodyChunks = new byte[(int)extendedNATSMessage.getProperties().get(MSG_SPLIT_COUNT)][];
                    bodyChunks[(int) extendedNATSMessage.getProperties().get(MSG_SPLIT_OID)] = extendedNATSMessage.getBody();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (bodyChunks!=null) {
            int bodySize = 0;
            for (byte[] bodyChunk : bodyChunks) {
                bodySize += bodyChunk.length;
            }
            byte[] reconstructedBody = new byte[bodySize];
            int idx = 0;
            for (byte[] bodyChunk : bodyChunks)
                for (byte bodyChunkB : bodyChunk) {
                    reconstructedBody[idx] = bodyChunkB;
                    idx++;
                }
            decodedMessage.put(MSG_BODY, reconstructedBody);
        }
        return decodedMessage;
    }
}
