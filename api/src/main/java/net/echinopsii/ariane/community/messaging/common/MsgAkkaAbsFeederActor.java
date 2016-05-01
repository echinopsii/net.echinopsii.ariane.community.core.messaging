/**
 * Messaging - Common Implementation
 * Message Feeder Actor Abstract implementation
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
import net.echinopsii.ariane.community.messaging.api.AppMsgFeeder;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;

public abstract class MsgAkkaAbsFeederActor extends UntypedActor {

    private MomMsgTranslator translator;
    private String        baseDest;
    private String        selector;
    private AppMsgFeeder  msgFeeder;

    private MomClient client ;

    public MsgAkkaAbsFeederActor(MomClient mclient, String bDest, String selector_,
                                 AppMsgFeeder feeder, MomMsgTranslator translator_) {
        client     = mclient;
        baseDest   = bDest;
        selector   = selector_;
        msgFeeder  = feeder;
        translator = translator_;
    }

    public MomMsgTranslator getTranslator() {
        return translator;
    }

    public String getBaseDest() {
        return baseDest;
    }

    public String getSelector() {
        return selector;
    }

    public AppMsgFeeder getMsgFeeder() {
        return msgFeeder;
    }

    public MomClient getClient() {
        return client;
    }

    public void setClient(MomClient client) {
        this.client = client;
    }
}