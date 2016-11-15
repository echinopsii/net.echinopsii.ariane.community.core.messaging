/**
 * Ariane Community Messaging
 * Mom Request Executor Interface
 * Copyright (C) 8/24/14 echinopsii
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

public interface MomRequestExecutor<Q, C extends AppMsgWorker> {

    /**
     * send request / no answer awaited
     * @param request the request message
     * @param destination the target destination queue
     * @return request message
     */
    public Map<String, Object> fireAndForget(Map<String, Object> request, Q destination);

    /**
     * send a request and get the answer
     * @param request the request message
     * @param destination the target destination queue
     * @param answerCB the callback object to treat the answer
     * @return the answer message
     */
    public Map<String, Object> RPC(Map<String, Object> request, Q destination, C answerCB);

    public Map<String, Object> RPC(Map<String, Object> request, String destination, String replySource, AppMsgWorker answerCB);
}