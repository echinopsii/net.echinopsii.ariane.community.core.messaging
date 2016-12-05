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

/**
 * MsgAkkaAbsFeederActor provides MoM provider agnostic method for akka feeder actor implementation.
 * It's intended to be extended by MoM provider specific feeder actors.
 */
public abstract class MsgAkkaAbsFeederActor extends UntypedActor {

    private MomMsgTranslator translator;
    private String        baseDest;
    private String        selector;
    private AppMsgFeeder  msgFeeder;

    private MomAkkaAbsClient client ;

    /**
     * Constructor
     * @param mclient the MomAkkaAbsClient to use with this feeder
     * @param bDest the feeder base destination
     * @param selector_ the feeder selector
     * @param feeder the app feeder this actor will use to generate message to be feeded
     * @param translator_ the translator used by this feeder
     */
    public MsgAkkaAbsFeederActor(MomAkkaAbsClient mclient, String bDest, String selector_,
                                 AppMsgFeeder feeder, MomMsgTranslator translator_) {
        client     = mclient;
        baseDest   = bDest;
        selector   = selector_;
        msgFeeder  = feeder;
        translator = translator_;
    }

    /**
     * @return the message translator attached to this feeder actor
     */
    public MomMsgTranslator getTranslator() {
        return translator;
    }

    /**
     * @return the base destination this feeder will publish message to
     */
    public String getBaseDest() {
        return baseDest;
    }

    /**
     * @return the selector this feeder will use to publish message
     */
    public String getSelector() {
        return selector;
    }

    /**
     * @return the message feeder generator used by this feeder
     */
    public AppMsgFeeder getMsgFeeder() {
        return msgFeeder;
    }

    /**
     * @return the MomClient attached to this feeder
     */
    public MomAkkaAbsClient getClient() {
        return client;
    }

    /**
     * @param client the MomAkkaAbsClient defined with this feeder actor.
     */
    public void setClient(MomAkkaAbsClient client) {
        this.client = client;
    }
}