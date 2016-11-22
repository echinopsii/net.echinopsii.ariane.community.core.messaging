/**
 * Messaging - Common Implementation
 * Service Akka implementation
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

import akka.actor.ActorRef;
import akka.actor.Cancellable;
import net.echinopsii.ariane.community.messaging.api.*;
import scala.concurrent.duration.Duration;

import java.util.concurrent.TimeUnit;

public class MomAkkaService implements MomService<ActorRef>{
    private MomMsgGroupSubServiceMgr msgGroupSubServiceMgr;
    private MomConsumer consumer;
    private ActorRef    msgWorker;
    private ActorRef    msgFeeder;
    private Cancellable cancellable;
    private MomAkkaAbsClient client;

    @Override
    public MomConsumer getConsumer() {
        return consumer;
    }

    @Override
    public MomAkkaService setConsumer(MomConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    @Override
    public ActorRef getMsgWorker() {
        return msgWorker;
    }

    @Override
    public MomAkkaService setMsgWorker(ActorRef msgWorker) {
        this.msgWorker = msgWorker;
        return this;
    }

    @Override
    public ActorRef getMsgFeeder() {
        return msgFeeder;
    }

    @Override
    public MomAkkaService setMsgFeeder(ActorRef msgFeeder, int schedulerInterval) {
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
    public MomMsgGroupSubServiceMgr getMsgGroupSubServiceMgr() {
        return msgGroupSubServiceMgr;
    }

    @Override
    public MomAkkaService setMsgGroupSubServiceMgr(MomMsgGroupSubServiceMgr groupMgr) {
        this.msgGroupSubServiceMgr = groupMgr;
        return this;
    }

    @Override
    public void stop() {
        if (consumer != null) consumer.stop();
        if (msgGroupSubServiceMgr !=null) msgGroupSubServiceMgr.stop();
        if (msgFeeder != null) client.getActorSystem().stop(msgFeeder);
        if (msgWorker !=null) client.getActorSystem().stop(msgWorker);
        if (cancellable != null) cancellable.cancel();
    }

    public MomClient getClient() {
        return client;
    }

    public MomAkkaService setClient(MomAkkaAbsClient client) {
        this.client = client;
        return this;
    }
}