/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
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

package net.echinopsii.ariane.community.core.messaging.rabbitmq;

import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.japi.Creator;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import net.echinopsii.ariane.community.core.messaging.api.AppMsgFeeder;

import java.io.IOException;
import java.util.Map;

public class MsgFeederActor extends UntypedActor {

    private MsgTranslator translator = new MsgTranslator();
    private String        baseDest;
    private String        selector;
    private AppMsgFeeder  msgFeeder;

    private Client        client ;
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
        client     = mclient;
        baseDest   = bDest;
        selector   = selector_;
        msgFeeder  = feeder;
        connection = client.getConnection();
        try {
            channel = connection.createChannel();
            channel.exchangeDeclare(this.baseDest, "topic");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof String && ((String)message).equals(AppMsgFeeder.MSG_FEED_NOW)) {
            Map<String, Object> newFeed = msgFeeder.apply();
            newFeed.put(MsgTranslator.MSG_APPLICATION_ID, client.getClientID());
            Message newFeedMsg = translator.encode(newFeed);
            channel.basicPublish(baseDest, selector, (com.rabbitmq.client.AMQP.BasicProperties) newFeedMsg.getProperties(), newFeedMsg.getBody());
        } else
            unhandled(message);
    }
}