/**
 * Messaging - RabbitMQ Implementation
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

package net.echinopsii.ariane.community.messaging.rabbitmq;

import akka.actor.Props;
import akka.japi.Creator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.echinopsii.ariane.community.messaging.api.AppMsgFeeder;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsFeederActor;

import java.io.IOException;
import java.util.Map;

public class MsgFeederActor extends MsgAkkaAbsFeederActor {

    private Connection    connection;
    private Channel       channel ;

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
        connection = (Connection)super.getClient().getConnection();
        try {
            channel = connection.createChannel();
            channel.exchangeDeclare(super.getBaseDest(), "topic");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof String && ((String)message).equals(AppMsgFeeder.MSG_FEED_NOW)) {
            Map<String, Object> newFeed = super.getMsgFeeder().apply();
            newFeed.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
            Message newFeedMsg = ((MsgTranslator)super.getTranslator()).encode(newFeed);
            channel.basicPublish(super.getBaseDest(), super.getSelector(), (com.rabbitmq.client.AMQP.BasicProperties) newFeedMsg.getProperties(), newFeedMsg.getBody());
        } else
            unhandled(message);
    }
}