/**
 * Ariane Community Messaging
 * Mom Message Group Subscriber Service Interface
 *
 * Copyright (C) 11/13/16 echinopsii
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
 * MomMsgGroupServiceMgr interface.
 *
 * Manage message group service through specific main service :
 * - store the opened message group service in a memory registry
 * - provide helper method to stop a message group service
 * - stop cleanly any message group service on final stop
 */
public interface MomMsgGroupServiceMgr {
    /**
     * open a message group service and attach it to this MomMsgGroupServiceMgr
     * @param groupID
     */
    void openMsgGroupService(String groupID);

    /**
     * close a message group service and detach it from this MomMsgGroupServiceMgr
     * @param groupID
     */
    void closeMsgGroupService(String groupID);

    /**
     * stop this MomMsgGroupServiceMgr and any non closed message group service still attached
     * to this MomMsgGroupServiceMgr.
     */
    void stop();
}
