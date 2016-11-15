/**
 * JSON ToolBox
 *
 * Copyright (C) 2014  Mathilde Ffrench
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

import com.fasterxml.jackson.core.JsonFactory;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.math.BigDecimal;

public class ToolBox {
    private static final Logger log = MomLoggerFactory.getLogger(ToolBox.class);

    public static JsonFactory jFactory = new JsonFactory();

    public static String getOuputStreamContent(ByteArrayOutputStream out, String encoding) throws IOException {
        ByteArrayInputStream input = new ByteArrayInputStream(out.toByteArray());
        BufferedReader br = new BufferedReader(new InputStreamReader(input, encoding));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
            sb.append('\n');
        }
        return sb.toString();
    }

    public static Object extractPropertyObjectValueFromString(String value, String type) throws IOException, PropertiesException {
        Object ovalue = null;
        switch (type.toLowerCase()) {
            case "int":
            case "integer":
                ovalue = new Integer(value);
                break;
            case "long":
                ovalue = new Long(value);
                break;
            case "double":
                ovalue = new Double(value);
                break;
            case "boolean":
                ovalue = new Boolean(value);
                break;
            case "decimal":
                ovalue = new BigDecimal(value);
                break;
            case "array":
            case "map":
                ovalue = PropertiesJSON.JSONStringToPropertyObject(type, value);
                break;
            case "string":
                ovalue = value;
                break;
            case "null":
                break;
            default:
                throw new PropertiesException("Invalid property type ("+type.toLowerCase()+"). Supported property types are : " +
                        "array, boolean, decimal, double, int, long, map, null and string");
        }
        return ovalue;
    }

}