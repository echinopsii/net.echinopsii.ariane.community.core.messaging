/**
 * Messaging - NATS Implementation
 * Message Feeder Actor
 * Copyright (C) 28/08/14 echinopsii
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
package net.echinopsii.ariane.community.messaging.nats;

import akka.actor.Props;
import akka.japi.Creator;
import io.nats.client.Connection;
import io.nats.client.Message;
import net.echinopsii.ariane.community.messaging.api.AppMsgFeeder;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsFeederActor;

import java.util.Map;

public class MsgFeederActor extends MsgAkkaAbsFeederActor {

    private Connection connection;
    private String subject;

    public static Props props(final Client mclient, final String baseDest, final String selector, final AppMsgFeeder feeder) {
        return Props.create(new Creator<MsgFeederActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgFeederActor create() throws Exception {
                return new MsgFeederActor(mclient, baseDest, selector, feeder);
            }
        });
    }

    public MsgFeederActor(Client mclient, String bDest, String selector_, AppMsgFeeder feeder) {
        super(mclient, bDest, selector_, feeder, new MsgTranslator());
        if (selector_ != null && !selector_.equals(""))
            subject = bDest + "." + selector_;
        else
            subject = bDest;
        connection = (Connection)super.getClient().getConnection();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof String && ((String)message).equals(AppMsgFeeder.MSG_FEED_NOW)) {
            Map<String, Object> newFeed = super.getMsgFeeder().apply();
            if (super.getClient().getClientID()!=null)
                newFeed.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
            Message newFeedMsg = ((MsgTranslator)super.getTranslator()).encode(newFeed);
            newFeedMsg.setSubject(subject);
            connection.publish(newFeedMsg);
        } else
            unhandled(message);
    }
}
