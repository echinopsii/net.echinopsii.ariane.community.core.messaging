/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
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

public interface MomService<A> {

    public MomConsumer getConsumer();
    public MomService setConsumer(MomConsumer consumer);

    public A getMsgWorker();
    public MomService setMsgWorker(A actorRef);

    public A getMsgFeeder();
    public MomService setMsgFeeder(A actorRef, int schedulerInterval);

    public void stop();
}