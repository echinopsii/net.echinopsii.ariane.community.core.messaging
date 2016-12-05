/**
 * Ariane Community Messaging
 * Mom Message Translator Interface
 *
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

package net.echinopsii.ariane.community.messaging.api;

import java.util.Map;

/**
 * MomMsgTranslator interface.
 * <p/>
 * Provide message fields, error codes and method definitions to encode / decode Map<String, Object> message to/from
 * specific MoM broker implementation.
 * <p/>
 * @param <M> the MoM broker message type
 */
public interface MomMsgTranslator<M> {

    String MSG_APPLICATION_ID = "MSG_APPLICATION_ID";
    String MSG_CORRELATION_ID = "MSG_CORRELATION_ID";
    String MSG_DELIVERY_MODE = "MSG_DELIVERY_MODE";
    String MSG_EXPIRATION    = "MSG_EXPIRATION";
    String MSG_MESSAGE_ID    = "MSG_MESSAGE_ID";
    String MSG_PRIORITY      = "MSG_PRIORITY";
    String MSG_REPLY_TO      = "MSG_REPLY_TO";
    String MSG_TIMESTAMP     = "MSG_TIMESTAMP";
    String MSG_TYPE          = "MSG_TYPE";
    String MSG_BODY          = "MSG_BODY";
    String MSG_PROPERTIES    = "MSG_PROPERTIES";
    String MSG_RETRY_COUNT   = "MSG_RETRY_COUNT";
    String MSG_TRACE         = "MSG_TRACE";

    String MSG_RC            = "RC";
    String MSG_ERR           = "SERVER_ERROR_MESSAGE";

    int MSG_RET_SUCCESS = 0;
    int MSG_RET_BAD_REQ = 400;
    int MSG_RET_NOT_FOUND = 404;
    int MSG_RET_SERVER_ERR = 500;

    String OPERATION_FDN = "OPERATION";
    String OPERATION_NOT_DEFINED = "NOT_DEFINED";

    Map<String, Class>  getMessageTypo();

    /**
     *
     * @param message
     * @return
     */
    M                   encode(Map<String, Object> message);

    /**
     *
     * @param message
     * @return
     */
    Map<String, Object> decode(M message);
}