/**
 * Messaging - NATS Implementation
 * Request Executor implementation
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

import io.nats.client.Connection;
import io.nats.client.Message;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsRequestExecutor;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RequestExecutor extends MomAkkaAbsRequestExecutor implements MomRequestExecutor<String, AppMsgWorker> {

    public RequestExecutor(Client client) throws IOException {
        super(client);
    }

    @Override
    public Map<String, Object> fireAndForget(Map<String, Object> request, String destination) {
        Message message = new MsgTranslator().encode(request);
        message.setSubject(destination);
        try {
            ((Connection)super.getMomClient().getConnection()).publish(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    @Override
    public Map<String, Object> RPC(Map<String, Object> request, String destination, String replySource, AppMsgWorker answerCB) {
        Map<String, Object> response = null;
        Message message = new MsgTranslator().encode(request);
        message.setSubject(destination);
        try {
            Message msgResponse = ((Connection)super.getMomClient().getConnection()).request(
                    message.getSubject(), message.getData()
            );
            response = new MsgTranslator().decode(msgResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (answerCB!=null)
            response = answerCB.apply(response);

        return response;
    }
}
