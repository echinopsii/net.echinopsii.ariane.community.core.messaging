/**
 * Messaging - RabbitMQ Implementation
 * Message Subscriber Actor
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

package net.echinopsii.ariane.community.messaging.common;

import akka.actor.UntypedActor;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;

public abstract class MsgAkkaAbsSubsActor extends UntypedActor {

    private MomMsgTranslator translator = null;
    private AppMsgWorker msgWorker   = null;

    public MsgAkkaAbsSubsActor(AppMsgWorker worker, MomMsgTranslator translator_) {
        msgWorker = worker;
        translator = translator_;
    }

    public MomMsgTranslator getTranslator() {
        return translator;
    }

    public AppMsgWorker getMsgWorker() {
        return msgWorker;
    }
}