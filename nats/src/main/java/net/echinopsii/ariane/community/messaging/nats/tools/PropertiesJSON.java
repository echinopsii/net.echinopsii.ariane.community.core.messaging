/**
 * Properties to JSON tooling
 *
 * Copyright (C) 14/01/14 echinopsii
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

package net.echinopsii.ariane.community.messaging.nats.tools;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.Map.Entry;

public class PropertiesJSON {

    private final static Logger log = LoggerFactory.getLogger(PropertiesJSON.class);

    private static void arrayListToJSON(ArrayList<Object> aobj, String objectName, JsonGenerator jgenerator) throws IOException {
        if (objectName!=null)
            jgenerator.writeArrayFieldStart(objectName);
        else
            jgenerator.writeStartArray();
        for (Object value : aobj) {
            if (value instanceof String) jgenerator.writeString((String) value);
            else if (value instanceof Long) jgenerator.writeNumber((Long) value);
            else if (value instanceof Integer) jgenerator.writeNumber((Integer) value);
            else if (value instanceof Double) jgenerator.writeNumber((Double) value);
            else if (value instanceof BigDecimal) jgenerator.writeNumber((BigDecimal) value);
            else if (value instanceof Boolean) jgenerator.writeBoolean((Boolean) value);
            else if (value instanceof ArrayList) arrayListToJSON((ArrayList<Object>) value, null, jgenerator);
            else if (value instanceof String[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((String[]) value)), objectName, jgenerator);
            else if (value instanceof Long[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Long[]) value)), objectName, jgenerator);
            else if (value instanceof Integer[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Integer[]) value)), objectName, jgenerator);
            else if (value instanceof Double[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Double[]) value)), objectName, jgenerator);
            else if (value instanceof BigDecimal[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((BigDecimal[]) value)), objectName, jgenerator);
            else if (value instanceof Boolean[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Boolean[]) value)), objectName, jgenerator);
            else if (value instanceof HashMap) hashMapToJSON((HashMap) value, null, jgenerator);
        }
        jgenerator.writeEndArray();
    }

    private static void hashMapToJSON(HashMap<String, Object> hobj, String objectName, JsonGenerator jgenerator) throws IOException {
        if (objectName!=null)
            jgenerator.writeObjectFieldStart(objectName);
        else
            jgenerator.writeStartObject();
        for (String key : hobj.keySet()) {
            Object value = hobj.get(key);
            log.debug("HashMap key {} value {}:{}", new Object[]{objectName, key, value.toString()});
            if (value instanceof String) jgenerator.writeStringField(key, (String) value);
            else if (value instanceof Long) jgenerator.writeNumberField(key, (Long) value);
            else if (value instanceof Integer) jgenerator.writeNumberField(key, (Integer) value);
            else if (value instanceof Double) jgenerator.writeNumberField(key, (Double) value);
            else if (value instanceof BigDecimal) jgenerator.writeNumberField(key, (BigDecimal) value);
            else if (value instanceof Boolean) jgenerator.writeBooleanField(key, (Boolean) value);
            else if (value instanceof HashMap) hashMapToJSON((HashMap) value, key, jgenerator);
            else if (value instanceof ArrayList) arrayListToJSON((ArrayList) value, key, jgenerator);
            else if (value instanceof String[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((String[]) value)), objectName, jgenerator);
            else if (value instanceof Long[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Long[]) value)), objectName, jgenerator);
            else if (value instanceof Integer[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Integer[]) value)), objectName, jgenerator);
            else if (value instanceof Double[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Double[]) value)), objectName, jgenerator);
            else if (value instanceof BigDecimal[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((BigDecimal[]) value)), objectName, jgenerator);
            else if (value instanceof Boolean[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Boolean[]) value)), objectName, jgenerator);
        }
        jgenerator.writeEndObject();
    }

    public static void propertiesToJSON(HashMap<String, Object> props, JsonGenerator jgenerator) throws IOException {
        if (props != null) {
            log.debug("Read properties {}", new Object[]{props.toString()});
            for (Entry<String, Object> current : props.entrySet()) {
                String objectName = current.getKey();
                Object obj = current.getValue();
                log.debug("Property {}", new Object[]{objectName});
                if (obj!=null) {
                    if (obj instanceof String) jgenerator.writeStringField(objectName, (String) obj);
                    else if (obj instanceof Boolean) jgenerator.writeBooleanField(objectName, (Boolean) obj);
                    else if (obj instanceof Long) jgenerator.writeNumberField(objectName, (Long) obj);
                    else if (obj instanceof Integer) jgenerator.writeNumberField(objectName, (Integer) obj);
                    else if (obj instanceof Double) jgenerator.writeNumberField(objectName, (Double) obj);
                    else if (obj instanceof BigDecimal) jgenerator.writeNumberField(objectName, (BigDecimal) obj);
                    else if (obj instanceof HashMap<?, ?>) hashMapToJSON((HashMap<String, Object>) obj, objectName, jgenerator);
                    else if (obj instanceof ArrayList<?>) arrayListToJSON((ArrayList) obj, objectName, jgenerator);
                    else if (obj instanceof String[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((String[]) obj)), objectName, jgenerator);
                    else if (obj instanceof Long[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Long[]) obj)), objectName, jgenerator);
                    else if (obj instanceof Integer[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Integer[]) obj)), objectName, jgenerator);
                    else if (obj instanceof Double[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Double[]) obj)), objectName, jgenerator);
                    else if (obj instanceof BigDecimal[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((BigDecimal[]) obj)), objectName, jgenerator);
                    else if (obj instanceof Boolean[]) arrayListToJSON(new ArrayList<Object>(Arrays.asList((Boolean[]) obj)), objectName, jgenerator);
                    else log.error("Property {} type is not managed...", new Object[]{objectName});
                } else log.error("Property {} value is null...", new Object[]{objectName});
            }
        }
    }





    private static HashMap<String, Object> JSONStringMapToPropertyObject(JsonNode rootTree, String json) throws PropertiesException {
        HashMap<String, Object> ret = null;
        if (rootTree.isObject()) {
            ObjectNode objectNode = (ObjectNode) rootTree;
            Iterator<Entry<String, JsonNode>> iter = objectNode.fields();
            while (iter.hasNext()) {
                Entry<String, JsonNode> entry = iter.next();
                String objectFieldName = entry.getKey();
                JsonNode objectField = entry.getValue();
                if (objectField.isArray()) {
                    if (ret == null)
                        ret = new HashMap<>();
                    ArrayNode arrayNode = (ArrayNode) objectField;
                    if (objectField.size() == 2) {
                        String vType = arrayNode.get(0).asText();
                        JsonNode subRootTree = arrayNode.get(1);
                        String value = arrayNode.get(1).asText();
                        switch (vType.toLowerCase()) {
                            case "boolean":
                                ret.put(objectFieldName, new Boolean(value));
                                break;
                            case "double":
                                ret.put(objectFieldName, new Double(value));
                                break;
                            case "decimal":
                                ret.put(objectFieldName, new BigDecimal(value));
                                break;
                            case "int":
                            case "integer":
                                ret.put(objectFieldName, new Integer(value));
                                break;
                            case "long":
                                ret.put(objectFieldName, new Long(value));
                                break;
                            case "string":
                                ret.put(objectFieldName, value);
                                break;
                            case "map":
                                HashMap<String, Object> valueHashMap = JSONStringMapToPropertyObject(subRootTree, json);
                                ret.put(objectFieldName, valueHashMap);
                                break;
                            case "array":
                                ArrayList<?> valueArray = JSONStringArrayToPropertyObject(subRootTree, json);
                                ret.put(objectFieldName, valueArray);
                                break;
                            default:
                                throw new PropertiesException("Unsupported map entry type (" + vType.toLowerCase() + "). " +
                                        "Supported types are : boolean, double, integer, long, string\n" +
                                        "Provided JSON : " + json
                                );
                        }
                    } else throw new PropertiesException("Json property map badly defined. " +
                            "Each map entry should be defined with following array : ['value type','value'].\n" +
                            "Provided JSON : " + json
                    );
                } else throw new PropertiesException("Json property map badly defined. " +
                        "Each map entry should be defined with following array : ['value type','value'].\n" +
                        "Provided JSON : " + json);
            }
        } else throw new PropertiesException("Json property badly defined : map should be defined as a Json object.\n" +
                "Provided JSON : " + json
        );
        return ret;
    }

    private static ArrayList<?> JSONStringArrayToPropertyObject(JsonNode rootTree, String json) throws PropertiesException {
        Object ret = null;
        if (rootTree.isArray()) {
            ArrayNode arrayNode = (ArrayNode) rootTree;
            if (arrayNode.size() == 2) {
                JsonNode arrayType = arrayNode.get(0);
                if (arrayType.isTextual()) {
                    String arrayTypeValue = arrayType.asText();
                    JsonNode value = arrayNode.get(1);
                    if (value.isArray()) {
                        ArrayNode arrayValue = (ArrayNode) value;
                        Iterator<JsonNode> iter = arrayValue.elements();
                        switch(arrayTypeValue.toLowerCase()) {
                            case "empty":
                                ret = new ArrayList<>();
                                break;
                            case "map":
                                ret = new ArrayList<HashMap<String, Object>>();
                                while (iter.hasNext()) {
                                    HashMap<String, Object> valueHashMap = JSONStringMapToPropertyObject(iter.next(), json);
                                    ((ArrayList<HashMap<String, Object>>) ret).add(valueHashMap);
                                }
                                break;
                            case "array":
                                ret = new ArrayList<ArrayList<?>>();
                                while (iter.hasNext()) {
                                    ArrayList<?> valueArray = JSONStringArrayToPropertyObject(iter.next(), json);
                                    ((ArrayList<ArrayList<?>>) ret).add(valueArray);
                                }
                                break;
                            case "boolean":
                                ret = new ArrayList<Boolean>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isBoolean())
                                        ((ArrayList)ret).add(next.asBoolean());
                                    else
                                        throw new PropertiesException("Json property array badly defined. Following array value is not a boolean : " +next.toString() + ".\n" +
                                                                      "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                                                                      "Provided JSON : " + json
                                        );
                                }
                                break;
                            case "decimal":
                                ret = new ArrayList<Double>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isBigDecimal())
                                        ((ArrayList)ret).add(next.decimalValue());
                                    else
                                        throw new PropertiesException("Json property array badly defined. Following array value is not a BigDecimal : " +next.toString() + ".\n" +
                                                                      "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                                                                      "Provided JSON : " + json
                                        );
                                }
                                break;
                            case "double":
                                ret = new ArrayList<Double>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isDouble())
                                        ((ArrayList)ret).add(next.asDouble());
                                    else
                                        throw new PropertiesException("Json property array badly defined. Following array value is not a double : " +next.toString() + ".\n" +
                                                                      "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                                                                      "Provided JSON : " + json
                                        );
                                }
                                break;
                            case "int":
                            case "integer":
                                ret = new ArrayList<Integer>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isInt())
                                        ((ArrayList)ret).add(next.asInt());
                                    else if (next.isBigInteger())
                                        ((ArrayList)ret).add(next.asInt());
                                    else
                                        throw new PropertiesException("Json property array badly defined. Following array value is not an integer : " +next.toString() + ".\n" +
                                                                      "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                                                                      "Provided JSON : " + json
                                        );
                                }
                                break;
                            case "long":
                                ret = new ArrayList<Long>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isLong())
                                        ((ArrayList)ret).add(next.asLong());
                                    else if (next.isInt())
                                        ((ArrayList)ret).add((long)(next.asInt()));
                                    else
                                        throw new PropertiesException("Json property array badly defined. Following array value is not a long : " +next.toString() + ".\n" +
                                                                       "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                                                                       "Provided JSON : " + json
                                        );
                                }
                                break;
                            case "object":
                                ret = new ArrayList<Object>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isTextual())
                                        ((ArrayList)ret).add(next.asText());
                                    else if (next.isBigDecimal())
                                        ((ArrayList)ret).add(next.decimalValue());
                                    else if (next.isBigInteger())
                                        ((ArrayList)ret).add(next.bigIntegerValue());
                                    else if (next.isBoolean())
                                        ((ArrayList)ret).add(next.booleanValue());
                                    else if (next.isDouble())
                                        ((ArrayList)ret).add(next.doubleValue());
                                    else if (next.isInt())
                                        ((ArrayList)ret).add(next.intValue());
                                    else if (next.isLong())
                                        ((ArrayList)ret).add(next.longValue());
                                    else if (next.isNumber())
                                        ((ArrayList)ret).add(next.numberValue());
                                    else if (next.isNull())
                                        ((ArrayList)ret).add(null);
                                }
                                break;
                            case "string":
                                ret = new ArrayList<String>();
                                while (iter.hasNext()) {
                                    JsonNode next = iter.next();
                                    if (next.isTextual())
                                        ((ArrayList)ret).add(next.asText());
                                    else
                                        throw new PropertiesException("Json property array badly defined. Following array value is not textual : " +next.toString() + ".\n" +
                                                                      "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                                                                      "Provided JSON : " + json
                                        );
                                }
                                break;
                            default:
                                throw new PropertiesException("Unsupported array type (" + arrayTypeValue.toLowerCase() + "). Supported types are : boolean, double, integer, long, string.\n" +
                                                              "Provided JSON : " + json);
                        }
                    } else throw new PropertiesException("Json property array badly defined. Array value is not an array.\n" +
                            "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                            "Provided JSON : " + json
                    );
                } else throw new PropertiesException("Json property array badly defined. Array type is not textual.\n" +
                        "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                        "Provided JSON : " + json
                );
            } else throw new PropertiesException("Json property array badly defined.\n" +
                    "Array entry should be defined with following array : ['array type',['value1','value2' ...]].\n" +
                    "Provided JSON : " + json
            );
        } else throw new PropertiesException("Json property badly defined : array should be defined as a Json array.\n" +
                "Provided JSON : " + json
        );
        return (ArrayList<?>) ret;
    }

    public static Object JSONStringToPropertyObject(String type, String json) throws IOException, PropertiesException {
        Object ret = null;
        ObjectMapper mapper = new ObjectMapper();
        JsonFactory factory = mapper.getFactory();
        JsonParser jp = factory.createJsonParser(json);
        JsonNode rootTree = mapper.readTree(jp);

        if (type.toLowerCase().equals("map")) ret = JSONStringMapToPropertyObject(rootTree, json);
        else if (type.toLowerCase().equals("array")) ret = JSONStringArrayToPropertyObject(rootTree, json);
        else throw new PropertiesException("Unsupported json type (" + type + "). Supported types are : array, map");

        return ret;
    }





    public static String getTypeFromObject(Object object) throws PropertiesException {
        String type = null;
        if (object instanceof String) type = "string";
        else if (object instanceof Long) type = "long";
        else if (object instanceof Integer) type = "int";
        else if (object instanceof Double) type = "double";
        else if (object instanceof BigDecimal) type = "decimal";
        else if (object instanceof Boolean) type = "boolean";
        else if (object instanceof List) type = "array";
        else if (object instanceof HashMap) type ="map";
        else if (object instanceof String[]) type = "array";
        else if (object instanceof Long[]) type = "array";
        else if (object instanceof Integer[]) type = "array";
        else if (object instanceof Double[]) type = "array";
        else if (object instanceof BigDecimal[]) type = "array";
        else if (object instanceof Boolean[]) type = "array";
        else throw new PropertiesException("Type " + object.getClass().getName() + " not supported !");
        return type;
    }

    private static void valueToJSON(Object value,  JsonGenerator jgenerator) throws IOException, PropertiesException {
        if (value instanceof String) jgenerator.writeString((String) value);
        else if (value instanceof Long) jgenerator.writeNumber((Long) value);
        else if (value instanceof Integer) jgenerator.writeNumber((Integer) value);
        else if (value instanceof Double) jgenerator.writeNumber((Double) value);
        else if (value instanceof BigDecimal) jgenerator.writeNumber((BigDecimal) value);
        else if (value instanceof Boolean) jgenerator.writeBoolean((Boolean) value);
        else if (value instanceof HashMap) hashMapToTypedHashMapJSON((HashMap) value, jgenerator);
        else if (value instanceof String[]) arrayListToTypedArrayJSON(new ArrayList<Object>(Arrays.asList((String[]) value)), "string", jgenerator);
        else if (value instanceof Long[]) arrayListToTypedArrayJSON(new ArrayList<Object>(Arrays.asList((Long[]) value)), "long", jgenerator);
        else if (value instanceof Integer[]) arrayListToTypedArrayJSON(new ArrayList<Object>(Arrays.asList((Integer[]) value)), "int", jgenerator);
        else if (value instanceof Double[]) arrayListToTypedArrayJSON(new ArrayList<Object>(Arrays.asList((Double[]) value)), "double", jgenerator);
        else if (value instanceof BigDecimal[]) arrayListToTypedArrayJSON(new ArrayList<Object>(Arrays.asList((BigDecimal[]) value)), "decimal", jgenerator);
        else if (value instanceof Boolean[]) arrayListToTypedArrayJSON(new ArrayList<Object>(Arrays.asList((Boolean[]) value)), "boolean", jgenerator);
        else if (value instanceof ArrayList) arrayListToTypedArrayJSON((ArrayList<Object>) value, "object", jgenerator);
    }

    private static void hashMapToTypedHashMapJSON(HashMap<String, Object> mobj, JsonGenerator jgenerator) throws IOException, PropertiesException {
        jgenerator.writeStartObject();
        for (String key : mobj.keySet()) {
            Object val = mobj.get(key);
            String type = getTypeFromObject(val);
            jgenerator.writeArrayFieldStart(key);
            jgenerator.writeString(type);
            valueToJSON(val, jgenerator);
            jgenerator.writeEndArray();
        }
        jgenerator.writeEndObject();
    }

    private static String hashMapToTypedHashMapJSONString(HashMap<String, Object> mobj) throws IOException, PropertiesException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonGenerator jgenerator = ToolBox.jFactory.createJsonGenerator(outStream, JsonEncoding.UTF8);
        hashMapToTypedHashMapJSON(mobj, jgenerator);
        jgenerator.close();
        return ToolBox.getOuputStreamContent(outStream, "UTF-8");
    }

    private static void arrayListToTypedArrayJSON(ArrayList<Object> aobj, String type, JsonGenerator jgenerator) throws IOException, PropertiesException {
        jgenerator.writeStartArray();
        if (aobj.size()!=0) {
            if (type.equals("object")) {
                boolean keepObjectType = false;
                String itemType = null;
                for(Object item : aobj) {
                    if (item != null) {
                        if (itemType==null)
                            itemType = getTypeFromObject(item);
                        else
                            if (!getTypeFromObject(item).equals(itemType)) {
                                keepObjectType = true;
                                break;
                            }
                    }
                }
                if (!keepObjectType) type = itemType;
            }
            if (type != null) jgenerator.writeString(type);
            jgenerator.writeStartArray();
            for (Object value : aobj) valueToJSON(value, jgenerator);
            jgenerator.writeEndArray();
        } else {
            jgenerator.writeString("empty");
            jgenerator.writeStartArray();
            jgenerator.writeEndArray();
        }
        jgenerator.writeEndArray();
    }

    private static String arrayListToTypedArrayJSONString(ArrayList<Object> aobj, String type) throws IOException, PropertiesException {
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        JsonGenerator jgenerator = ToolBox.jFactory.createJsonGenerator(outStream, JsonEncoding.UTF8);
        arrayListToTypedArrayJSON(aobj, type, jgenerator);
        jgenerator.close();
        return ToolBox.getOuputStreamContent(outStream, "UTF-8");
    }

    public static TypedPropertyField propertyFieldToTypedPropertyField(String name, Object obj) throws IOException, PropertiesException {
        TypedPropertyField typedPropertyField = null;
        if (obj instanceof String) typedPropertyField = new TypedPropertyField(name, "string", obj.toString());
        else if (obj instanceof Boolean)  typedPropertyField = new TypedPropertyField(name, "boolean", obj.toString());
        else if (obj instanceof Long) typedPropertyField = new TypedPropertyField(name, "long", obj.toString());
        else if (obj instanceof Integer) typedPropertyField = new TypedPropertyField(name, "int", obj.toString());
        else if (obj instanceof Double) typedPropertyField = new TypedPropertyField(name, "double", obj.toString());
        else if (obj instanceof BigDecimal) typedPropertyField = new TypedPropertyField(name, "decimal", obj.toString());
        else if (obj instanceof Map<?, ?>) typedPropertyField = new TypedPropertyField(name, "map", hashMapToTypedHashMapJSONString((HashMap) obj));
        else if (obj instanceof List<?>) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "object"));
        else if (obj instanceof String[]) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "string"));
        else if (obj instanceof Long[]) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "long"));
        else if (obj instanceof Integer[]) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "int"));
        else if (obj instanceof Double[]) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "double"));
        else if (obj instanceof BigDecimal[]) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "decimal"));
        else if (obj instanceof Boolean[]) typedPropertyField = new TypedPropertyField(name, "array", arrayListToTypedArrayJSONString((ArrayList<Object>) obj, "boolean"));
        else log.error("Property {} type is not managed...", new Object[]{name});
        return typedPropertyField;
    }

    public static List<TypedPropertyField> propertiesToTypedPropertiesList(Map<String, Object> props) throws IOException, PropertiesException {
        List<TypedPropertyField> list = new ArrayList<>();
        for (String key : props.keySet()) {
            Object obj = props.get(key);
            if (obj!=null) {
                TypedPropertyField typedPropertyField = propertyFieldToTypedPropertyField(key, obj);
                if (typedPropertyField!=null) list.add(typedPropertyField);
            } else log.error("Property {} value is null...", new Object[]{key});
        }
        return list;
    }




    public static class TypedPropertyField {
        String propertyName;
        String propertyType;
        String propertyValue;

        public TypedPropertyField() {
        }

        public TypedPropertyField(String name, String type, String value) {
            this.propertyName = name;
            this.propertyType = type;
            this.propertyValue = value;
        }

        public String getPropertyName() {
            return propertyName;
        }

        public void setPropertyName(String propertyName) {
            this.propertyName = propertyName;
        }

        public String getPropertyType() {
            return propertyType;
        }

        public void setPropertyType(String propertyType) {
            this.propertyType = propertyType;
        }

        public String getPropertyValue() {
            return propertyValue;
        }

        public void setPropertyValue(String propertyValue) {
            this.propertyValue = propertyValue;
        }

        public void toJSON(JsonGenerator jgenerator) throws IOException {
            jgenerator.writeStartObject();
            jgenerator.writeStringField("propertyName", this.propertyName);
            jgenerator.writeStringField("propertyType", this.propertyType);
            jgenerator.writeStringField("propertyValue", this.propertyValue);
            jgenerator.writeEndObject();
        }

        public String toJSONString() throws IOException {
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            JsonGenerator jgenerator = ToolBox.jFactory.createJsonGenerator(outStream, JsonEncoding.UTF8);
            toJSON(jgenerator);
            jgenerator.close();
            return ToolBox.getOuputStreamContent(outStream, "UTF-8");
        }
    }

    public static TypedPropertyField typedPropertyFieldFromJSON(String payload) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(payload, TypedPropertyField.class);
        } catch (JsonMappingException exception) {
            log.error("Deserialization error with payload : " + payload);
            throw exception;
        }
    }

    public static Object getValueFromTypedPropertyField(TypedPropertyField typedPropertyField) throws IOException, PropertiesException {
        Object value;
        switch (typedPropertyField.getPropertyType()) {
            case "string":
                value = typedPropertyField.getPropertyValue();
                break;
            case "int":
            case "integer":
                value = new Integer(typedPropertyField.getPropertyValue());
                break;
            case "long":
                value = new Long(typedPropertyField.getPropertyValue());
                break;
            case "double":
                value = new Double(typedPropertyField.getPropertyValue());
                break;
            case "decimal":
                value = new BigDecimal(typedPropertyField.getPropertyValue());
                break;
            case "boolean":
                value = new Boolean(typedPropertyField.getPropertyValue());
                break;
            case "array":
            case "map":
                value = PropertiesJSON.JSONStringToPropertyObject(typedPropertyField.getPropertyType(), typedPropertyField.getPropertyValue());
                break;
            default:
                throw new PropertiesException("Unsupported json type (" + typedPropertyField.getPropertyType() + "). " +
                        "Supported types are : array, boolean, decimal, double, integer, long, map");
        }
        return value;
    }
}