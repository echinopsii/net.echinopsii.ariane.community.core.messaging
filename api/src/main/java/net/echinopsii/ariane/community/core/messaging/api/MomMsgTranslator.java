/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
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

package net.echinopsii.ariane.community.core.messaging.api;

import java.util.Map;

public interface MomMsgTranslator<M> {

    public final String MSG_APPLICATION_ID = "MSG_APPLICATION_ID";
    public final String MSG_CORRELATION_ID = "MSG_CORRELATION_ID";
    public final String MSG_DELIVERY_MODE = "MSG_DELIVERY_MODE";
    public final String MSG_EXPIRATION    = "MSG_EXPIRATION";
    public final String MSG_MESSAGE_ID    = "MSG_MESSAGE_ID";
    public final String MSG_PRIORITY      = "MSG_PRIORITY";
    public final String MSG_REPLY_TO      = "MSG_REPLY_TO";
    public final String MSG_TIMESTAMP     = "MSG_TIMESTAMP";
    public final String MSG_TYPE          = "MSG_TYPE";
    public final String MSG_BODY          = "MSG_BODY";

    public Map<String, Class>  getMessageTypo();

    public M                   encode(Map<String, Object> message);
    public Map<String, Object> decode(M message);
}