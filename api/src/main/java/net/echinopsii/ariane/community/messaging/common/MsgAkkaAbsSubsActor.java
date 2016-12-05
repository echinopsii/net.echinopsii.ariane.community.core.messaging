/**
 * Messaging - Common Implementation
 * Message Subscriber Actor Abstract Implementation
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

import akka.actor.UntypedActor;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;

/**
 * MsgAkkaAbsSubActor provides MoM provider agnostic method implementations for akka subscription actor implementation.
 */
public abstract class MsgAkkaAbsSubsActor extends UntypedActor {

    private MomMsgTranslator translator = null;
    private AppMsgWorker msgWorker   = null;

    /**
     * Constructor
     * @param worker the message worker in charge of message treatment coming from the subscription
     * @param translator_ the message translator to be used with this subscription actor
     */
    public MsgAkkaAbsSubsActor(AppMsgWorker worker, MomMsgTranslator translator_) {
        msgWorker = worker;
        translator = translator_;
    }

    /**
     * @return the message translator attached to this subscription actor
     */
    public MomMsgTranslator getTranslator() {
        return translator;
    }

    /**
     * @return the message worker in charge of message treatment
     */
    public AppMsgWorker getMsgWorker() {
        return msgWorker;
    }
}