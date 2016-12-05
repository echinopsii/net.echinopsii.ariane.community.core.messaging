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
import java.util.concurrent.TimeoutException;

/**
 * MomRequestExecutor interface
 *
 * Provide simple method definition for :
 * - fire and forget request (no answer awaited)
 * - remote procedure call request where the answer will be treated by provided answer worker.
 *
 * @param <Q> type of destination / source (could be a String or specific MoM queue)
 * @param <W> type of AppMsgWorker (where you push your business logic)
 */
public interface MomRequestExecutor<Q, W extends AppMsgWorker> {

    /**
     * send request / no answer awaited
     * @param request the request message
     * @param destination the target destination queue
     * @return the request message
     */
    Map<String, Object> FAF(Map<String, Object> request, Q destination);

    /**
     * send a request and get the answer
     * @param request the request message
     * @param destination the target destination queue
     * @param answerWorker the worker object to treat the answer
     * @return the answer message
     */
    Map<String, Object> RPC(Map<String, Object> request, Q destination, W answerWorker) throws TimeoutException;

    /**
     * send a request and get the answer from specified answer source
     * @param request the request message
     * @param destination the target destination queue
     * @param answerSource the source to get the answer from
     * @param answerWorker the worker object to treat the answer
     * @return the answer message
     * @throws TimeoutException
     */
    Map<String, Object> RPC(Map<String, Object> request, Q destination, Q answerSource, W answerWorker) throws TimeoutException;
}