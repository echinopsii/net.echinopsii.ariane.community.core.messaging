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
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;

import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * MomAkkaAbsRequestExecutor provides an abstract implementation of MomRequestExecutor interface based on actors model and Akka.
 */
public abstract class MomAkkaAbsRequestExecutor implements MomRequestExecutor<String, AppMsgWorker> {

    private MomAkkaAbsClient momClient;

    /**
     * Constructor
     * @param client : the MomClient this request executor will work with
     */
    public MomAkkaAbsRequestExecutor(MomAkkaAbsClient client) {
        momClient = client;
    }

    /**
     * @return the MomAkkaAbsClient defined with this request executor.
     */
    public MomAkkaAbsClient getMomClient() {
        return momClient;
    }

    /**
     * Remote procedure call
     * @param request the request message
     * @param destination the target destination queue
     * @param answerWorker the worker object to treat the answer
     * @return the reply of this rpc
     * @throws TimeoutException when no reply comes after momClient.rpcRetry * momClient.rpcTimeout (sec)
     */
    @Override
    public Map<String, Object> RPC(Map<String, Object> request, String destination, AppMsgWorker answerWorker) throws TimeoutException {
        return RPC(request, destination, null, answerWorker);
    }
}