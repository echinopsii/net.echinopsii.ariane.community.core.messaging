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

/**
 * MsgFeederActor class extending {@link net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsFeederActor} abstract class for RabbitMQ MoM
 */
public class MsgFeederActor extends MsgAkkaAbsFeederActor {

    private Connection    connection;
    private Channel       channel ;

    /**
     * (internal usage only)
     * Return Akka actor Props to spawn a new MsgFeederActor through Akka.
     * Should not be called outside {@link net.echinopsii.ariane.community.messaging.rabbitmq.ServiceFactory#feederService(String, String, int, AppMsgFeeder)}
     *
     * @param mclient the initialized RabbitMQ client
     * @param baseDest the base destination of this feeder
     * @param selector the selector of this feeder (can be null)
     * @param feeder the AppMsgFeeder in charge of message feeding
     * @return Akka actor Props
     */    public static Props props(final Client mclient, final String baseDest, final String selector, final AppMsgFeeder feeder) {
        return Props.create(new Creator<MsgFeederActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgFeederActor create() throws Exception {
                return new MsgFeederActor(mclient, baseDest, selector, feeder);
            }
        });
    }

    /**
     * (internal usage only)
     * MsgFeederActor constructor. Should not be called outside {@link this#props}
     * Define the target subject as bDest.selector_ if selector_ is not null else as bDest
     *
     * @param mclient the initialized RabbitMQ client
     * @param bDest the base destination of this feeder
     * @param selector_ the selector of this feeder (can be null)
     * @param feeder the AppMsgFeeder in charge of message feeding
     */
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

    /**
     * {@link akka.actor.UntypedActor#onReceive(Object)} implementation.
     * if message is {@link net.echinopsii.ariane.community.messaging.api.AppMsgFeeder#MSG_FEED_NOW} then request new message from feeder
     * and publish it on this MsgFeederActor subject.
     * else unhandled
     *
     * @param message the akka message received by actor
     * @throws IOException if problem encountered while publishing message
     */
    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof String && message.equals(AppMsgFeeder.MSG_FEED_NOW)) {
            Map<String, Object> newFeed = super.getMsgFeeder().apply();
            if (super.getClient().getClientID()!=null)
                newFeed.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
            Message newFeedMsg = ((MsgTranslator)super.getTranslator()).encode(newFeed);
            channel.basicPublish(super.getBaseDest(), super.getSelector(), (com.rabbitmq.client.AMQP.BasicProperties) newFeedMsg.getProperties(), newFeedMsg.getBody());
        } else
            unhandled(message);
    }
}