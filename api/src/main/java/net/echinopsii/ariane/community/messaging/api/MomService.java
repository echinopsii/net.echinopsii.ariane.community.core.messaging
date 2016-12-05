/**
 * Ariane Community Messaging
 * Mom Service Interface
 *
 * Copyright (C) 8/27/14 echinopsii
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

/**
 * MomService interface.
 * <p/>
 * The service in charge of :
 * <br/>
 * - consuming message from Mom Broker resource (queue or subscription) and pushing to the actor in charge of the
 * message business treatment in the case of a request service.
 * <br/>
 * OR
 * <br/>
 * - feeding data regularly on a topic in the case of a feeder service
 * <p/>
 * @see net.echinopsii.ariane.community.messaging.api.MomServiceFactory
 * <p/>
 * In the case this service is a request service, a Message Group Service Manager can be attached to this service
 * to manage message treatment coming from a message group.
 * @see net.echinopsii.ariane.community.messaging.api.MomMsgGroupServiceMgr
 * <p/>
 * @param <R> type of running reference (could be a thread id or an actor ref ....)
 * <p/>
 * Implementation with akka actors :
 * @see net.echinopsii.ariane.community.messaging.common.MomAkkaService
 * */
public interface MomService<R> {
    /**
     * @return the service message feeder actor if this is a message feeder service else null
     */
    R getMsgFeeder();

    /**
     * @param runningRef from message feeder to associate with this service
     * @param schedulerInterval
     * @return
     */
    MomService setMsgFeeder(R runningRef, int schedulerInterval);

    /**
     * @return MomConsumer associated with this service if this is a request service else null
     */
    MomConsumer getConsumer();

    /**
     * @param consumer to associate with this request service
     * @return this service
     */
    MomService setConsumer(MomConsumer consumer);

    /**
     * @return message worker actor associated with this service if this is a request service else null
     */
    R getMsgWorker();

    /**
     * @param runningRef from message worker to associate with this service
     * @return
     */
    MomService setMsgWorker(R runningRef);

    /**
     * @return associated message group services manager if this is a message request service else null
     */
    MomMsgGroupServiceMgr getMsgGroupServiceMgr();

    /**
     * @param groupMgr the message group services manager to associate
     * @return this service
     */
    MomService setMsgGroupServiceMgr(MomMsgGroupServiceMgr groupMgr);

    /**
     * cleanly stop this service as such as its underlying resources
     */
    void stop();
}