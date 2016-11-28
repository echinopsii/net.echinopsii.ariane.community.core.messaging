/**
 * Messaging - Common Implementation
 * Client abstract implementation
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
import akka.actor.ActorSystem;
import akka.actor.DeadLetter;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.api.MomService;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;

import java.util.*;

public abstract class MomAkkaAbsClient implements MomClient {

    private static ActorRef dlLogger = null;

    private ActorSystem system      = null;
    private ActorRef mainSupervisor = null;
    private String      clientID    = null;

    private boolean msgDebugOnTimeout = false;
    private int nbRouteesPerService = 5;

    private long rpcTimeout = 10;
    private int rpcRetry = 3;

    private MomServiceFactory serviceFactory ;
    private List<MomRequestExecutor> requestExecutors = new ArrayList<MomRequestExecutor>();

    private HashMap<String, Long> sessionThreadRegistry = new HashMap<>();
    private HashMap<Long, String> threadSessionRegistry = new HashMap<>();


    @Override
    public void init(Properties properties) throws Exception {
        Dictionary props = new Properties();
        for(Object key : properties.keySet())
            props.put(key, properties.get(key));
        init(props);
    }

    public ActorSystem getActorSystem() {
        return system;
    }

    public void setActorSystem(ActorSystem sys) {
        String mainSupName;
        if (this.clientID!=null) mainSupName = this.clientID.replace(" ", "_") + "_main_supervisor";
        else mainSupName = UUID.randomUUID() + "_main_supervisor";
        this.system = sys;
        if (dlLogger==null) {
            dlLogger = this.system.actorOf(MomAkkaDLLogger.props(), "DLLogger");
            this.system.eventStream().subscribe(dlLogger, DeadLetter.class);
        }
        this.mainSupervisor = this.system.actorOf(MomAkkaSupervisor.props(), mainSupName);
    }

    public ActorRef getMainSupervisor() {
        return mainSupervisor;
    }

    public void preCloseMainSupervisor() {
        MomAkkaSupervisor.willStopSoon(mainSupervisor);
    }

    public void closeMainSupervisor() {
        this.system.stop(mainSupervisor);
        if (dlLogger!=null) {
            this.system.stop(dlLogger);
            dlLogger = null;
        }
    }

    @Override
    public String getClientID() {
        return clientID;
    }

    public void setClientID(String id) {
        this.clientID = id;
    }

    @Override
    public boolean isMsgDebugOnTimeout() {
        return msgDebugOnTimeout;
    }

    @Override
    public void setMsgDebugOnTimeout(boolean msgDebugOnTimeout) {
        this.msgDebugOnTimeout = msgDebugOnTimeout;
    }

    @Override
    public int getNbRouteesPerService() {
        return nbRouteesPerService;
    }

    @Override
    public void setNbRouteesPerService(int nbRouteesPerService) {
        this.nbRouteesPerService = nbRouteesPerService;
    }

    @Override
    public long getRPCTimout() {
        return this.rpcTimeout;
    }

    @Override
    public void setRPCTimout(long rpcTimout) {
        if (rpcTimout > 0) this.rpcTimeout = rpcTimout;
        else this.rpcTimeout = -1L;
    }

    @Override
    public int getRPCRetry() {
        return this.rpcRetry;
    }

    @Override
    public void setRPCRetry(int rpcRetry) {
        this.rpcRetry = rpcRetry;
    }

    @Override
    public MomServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    public void setServiceFactory(MomServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    public List<MomRequestExecutor> getRequestExecutors() {
        return requestExecutors;
    }

    @Override
    public void openMsgGroupRequest(String groupID) {
        Long threadID = Thread.currentThread().getId();
        this.sessionThreadRegistry.put(groupID, threadID);
        this.threadSessionRegistry.put(threadID, groupID);
    }

    @Override
    public String getCurrentMsgGroup() {
        Long threadID = Thread.currentThread().getId();
        return threadSessionRegistry.get(threadID);
    }

    @Override
    public void closeMsgGroupRequest(String groupID) {
        Long threadID = this.sessionThreadRegistry.get(groupID);
        if (threadID!=null) {
            this.threadSessionRegistry.remove(threadID);
            this.sessionThreadRegistry.remove(groupID);
        }
    }

    HashMap<String, ActorRef> msgGroupSupervisors = new HashMap<>();

    public ActorRef getMsgGroupSupervisor(String groupID) {
        return msgGroupSupervisors.get(groupID);
    }

    public void preCloseMsgGroupSupervisors() {
        for (ActorRef supervisor : msgGroupSupervisors.values()) MomAkkaSupervisor.willStopSoon(supervisor);
    }

    public void closeMsgGroupSupervisors() {
        for (ActorRef supervisor : msgGroupSupervisors.values()) this.system.stop(supervisor);
    }

    @Override
    public void openMsgGroupService(String groupID) {
        if (this.getServiceFactory()!=null) {
            msgGroupSupervisors.put(
                    groupID,
                    this.system.actorOf(MomAkkaSupervisor.props(), groupID + "_msggroup_supervisor")
            );
            for (MomService service : ((MomAkkaAbsServiceFactory) this.getServiceFactory()).getServices())
                if (service.getMsgGroupServiceMgr() != null)
                    service.getMsgGroupServiceMgr().openMsgGroupService(groupID);
        }
    }

    @Override
    public void closeMsgGroupService(String groupID) {
        if (this.getServiceFactory()!=null) {
            ActorRef msgGroupSupervisor = this.getMsgGroupSupervisor(groupID);
            if (msgGroupSupervisor!=null) MomAkkaSupervisor.willStopSoon(msgGroupSupervisor);
            for (MomService service : ((MomAkkaAbsServiceFactory) this.getServiceFactory()).getServices())
                if (service.getMsgGroupServiceMgr() != null)
                    service.getMsgGroupServiceMgr().closeMsgGroupService(groupID);
            if (msgGroupSupervisor!=null) {
                this.system.stop(msgGroupSupervisor);
                this.msgGroupSupervisors.remove(groupID);
            }
        }
    }
}