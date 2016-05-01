/**
 * Messaging - Common Implementation
 * Request Executor abstract implementation
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

package net.echinopsii.ariane.community.messaging.common;

import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class MomAkkaAbsRequestExecutor implements MomRequestExecutor<String, AppMsgWorker> {

    private MomClient momClient;
    private Map<String, Object> consumers = new HashMap<String, Object>();

    public MomAkkaAbsRequestExecutor(MomClient client) throws IOException {
        momClient = client;
    }

    public void stop() throws IOException {
        consumers.clear();
    }

    public MomClient getMomClient() {
        return momClient;
    }

    public Map<String, Object> getConsumers() {
        return consumers;
    }

    @Override
    public Map<String, Object> RPC(Map<String, Object> request, String destination, AppMsgWorker answerCB) {
        return RPC(request, destination, null, answerCB);
    }
}