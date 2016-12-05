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

/**
 * MomAkkaService implements MomService with akka actors
 */
public class MomAkkaService implements MomService<ActorRef>{
    private MomMsgGroupServiceMgr msgGroupServiceMgr;
    private MomConsumer consumer;
    private ActorRef    msgWorker;
    private ActorRef    msgFeeder;
    private Cancellable cancellable;
    private MomAkkaAbsClient client;

    /**
     * @return message feeder actor ref associater with this service if this is a feeder service else null
     */
    @Override
    public ActorRef getMsgFeeder() {
        return msgFeeder;
    }

    /**
     * @param actorRef
     * @param schedulerInterval
     * @return
     */
    @Override
    public MomAkkaService setMsgFeeder(ActorRef actorRef, int schedulerInterval) {
        this.msgFeeder = actorRef;
        cancellable = client.getActorSystem().scheduler().schedule(Duration.Zero(),
                Duration.create(schedulerInterval, TimeUnit.MILLISECONDS),
                actorRef,
                AppMsgFeeder.MSG_FEED_NOW,
                client.getActorSystem().dispatcher(),
                null);
        return this;
    }

    /**
     * @return MomConsumer associated with this service if this is a request service else null
     */
    @Override
    public MomConsumer getConsumer() {
        return consumer;
    }

    /**
     * @param consumer to assiocate with this service
     * @return
     */
    @Override
    public MomAkkaService setConsumer(MomConsumer consumer) {
        this.consumer = consumer;
        return this;
    }

    /**
     * @return message worker akka actor ref associated with this service if this is a request service else null
     */
    @Override
    public ActorRef getMsgWorker() {
        return msgWorker;
    }

    /**
     * @param actorRef akka actor ref from message worker to associate with this service
     * @return this
     */
    @Override
    public MomAkkaService setMsgWorker(ActorRef actorRef) {
        this.msgWorker = actorRef;
        return this;
    }

    /**
     * @return message group service managerif this service is a request service providing message grouping possibility else null
     */
    @Override
    public MomMsgGroupServiceMgr getMsgGroupServiceMgr() {
        return msgGroupServiceMgr;
    }

    /**
     * @param groupMgr the message group services manager to associate
     * @return this service
     */
    @Override
    public MomAkkaService setMsgGroupServiceMgr(MomMsgGroupServiceMgr groupMgr) {
        this.msgGroupServiceMgr = groupMgr;
        return this;
    }

    /**
     * Stop this service and attached resources
     */
    @Override
    public void stop() {
        if (consumer != null) consumer.stop();
        if (msgGroupServiceMgr !=null) msgGroupServiceMgr.stop();
        if (msgFeeder != null) client.getActorSystem().stop(msgFeeder);
        if (msgWorker !=null) client.getActorSystem().stop(msgWorker);
        if (cancellable != null) cancellable.cancel();
    }

    /**
     * @return the MomAkkaAbsClient defined with this service.
     */
    public MomAkkaAbsClient getClient() {
        return client;
    }

    /**
     * @param client the MomAkkaAbsClient defined with this service.
     * @return this service
     */
    public MomAkkaService setClient(MomAkkaAbsClient client) {
        this.client = client;
        return this;
    }
}