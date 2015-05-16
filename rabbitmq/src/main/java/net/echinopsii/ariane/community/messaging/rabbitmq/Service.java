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

package net.echinopsii.ariane.community.messaging.rabbitmq;

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import net.echinopsii.ariane.community.core.messaging.api.AppMsgFeeder;
import net.echinopsii.ariane.community.core.messaging.api.MomConsumer;
import net.echinopsii.ariane.community.core.messaging.api.MomService;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class Service implements MomService<ActorRef>{

    private MomConsumer consumer;
    private ActorRef    msgWorker;
    private ActorRef    msgFeeder;
    private Cancellable cancellable;
    private Client      client;


    @Override
    public MomConsumer getConsumer() {
        return consumer;
    }

    @Override
    public Service setConsumer(MomConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    @Override
    public ActorRef getMsgWorker() {
        return msgWorker;
    }

    @Override
    public Service setMsgWorker(ActorRef msgWorker) {
        this.msgWorker = msgWorker;
        return this;
    }

    @Override
    public ActorRef getMsgFeeder() {
        return msgFeeder;
    }

    @Override
    public Service setMsgFeeder(ActorRef msgFeeder, int schedulerInterval) {
        this.msgFeeder = msgFeeder;
        cancellable = client.getActorSystem().scheduler().schedule(Duration.Zero(),
                                                                   Duration.create(schedulerInterval, TimeUnit.MILLISECONDS),
                                                                   msgFeeder,
                                                                   AppMsgFeeder.MSG_FEED_NOW,
                                                                   client.getActorSystem().dispatcher(),
                                                                   null);
        return this;
    }

    @Override
    public void stop() {
        if (consumer != null) consumer.stop();
        if (cancellable != null) cancellable.cancel();
        if (msgFeeder != null) client.getActorSystem().stop(msgFeeder);
        if (msgWorker !=null) client.getActorSystem().stop(msgWorker);
    }

    public Client getClient() {
        return client;
    }

    public Service setClient(Client client) {
        this.client = client;
        return this;
    }
}