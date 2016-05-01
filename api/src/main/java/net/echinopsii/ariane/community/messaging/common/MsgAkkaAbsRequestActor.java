/**
 * Messaging - Common Implementation
 * Message Request Actor Abstract Implementation
 * Copyright (C) 04/30/16 echinopsii
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
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;


public abstract class MsgAkkaAbsRequestActor extends UntypedActor {

    private MomMsgTranslator translator = null;
    private AppMsgWorker msgWorker   = null;
    private MomClient client      = null;

    public MsgAkkaAbsRequestActor(MomClient mclient, AppMsgWorker worker, MomMsgTranslator translator_) {
        client = mclient;
        msgWorker = worker;
        translator = translator_;
    }

    public MomMsgTranslator getTranslator() {
        return translator;
    }

    public AppMsgWorker getMsgWorker() {
        return msgWorker;
    }

    public MomClient getClient() {
        return client;
    }

    public void setClient(MomClient client) {
        this.client = client;
    }
}