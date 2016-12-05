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

/**
 * MomAkkaAbsClient provides an abstract implementation of MomClient interface based on actors model and Akka.
 * <p/>
 * As a MomClient abstract implementation, it provides MoM provider agnostic tooling implementation like :
 * <br/>- the clientID definition
 * <br/> - link to the ServiceFactory to create new services besides this MomClient
 * <br/> - registries for requests executors, messages groups requests and messages groups services
 * <br/> - MomClient configuration with setter/getter. Default values :
 * <br/> --- msgDebugOnTimeout = false
 * <br/> --- rpcTimeout = 10 (sec)
 * <br/> --- rpcRetry   = 3
 * <p/>
 * MomAkkaAbsClient use actors model pattern to manage the MomClient entities (services or requests executors).
 * <br/> On top of the MomClient actors hierarchy you will find :
 * <br/> - the akka system actors (root guardian, system guardian and user guardian).
 * <br/> - a dead letter logger actor to print any lost message in the system (parent is user guardian)
 * <br/> - a main supervisor dedicated to supervise and cleanly close the MomClient main services and requests executors actors.
 * <br/> - message group services supervisors to supervise and cleanly close the MomClient services dedicated to a
 * message group.
 * <p/>
 * MomAkkaAbsClient also provide an actor specific configuration for services : the routees count per service.
 * Indeed when creating new service you may want to define several worker actors to dispatch treatment load on these
 * actors. To do so Akka provide router/routees actors pattern to help (the router will forward request to the routees
 * - round robin fashion by default). Default value for this parameter is 5.
 *
 */
public abstract class MomAkkaAbsClient implements MomClient {

    private static ActorRef dlLogger = null;

    private ActorSystem system      = null;
    private ActorRef mainSupervisor = null;
    private String      clientID    = null;

    private boolean msgDebugOnTimeout = false;
    private int routeesCountPerService = 5;
    private long rpcTimeout = 10;
    private int rpcRetry = 3;

    private MomServiceFactory serviceFactory ;

    private List<MomRequestExecutor> requestExecutors = new ArrayList<>();
    private HashMap<String, Long> msgGroupThreadRegistry = new HashMap<>();
    private HashMap<Long, String> threadMsgGroupRegistry = new HashMap<>();
    private HashMap<String, ActorRef> msgGroupSupervisors = new HashMap<>();


    /**
     * initialize this MomAkkaAbsClient with provided properties
     * @param properties
     * @throws Exception
     */
    @Override
    public void init(Properties properties) throws Exception {
        Dictionary props = new Properties();
        for(Object key : properties.keySet())
            props.put(key, properties.get(key));
        init(props);
    }

    /**
     * @return defined actor system
     */
    public ActorSystem getActorSystem() {
        return system;
    }

    /**
     * set actor system and by the way init the dead letter logger and mainSupervisor actors
     * @param sys
     */
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

    /**
     * @return the main supervisoer actor ref.
     */
    public ActorRef getMainSupervisor() {
        return mainSupervisor;
    }

    /**
     * pre close main supervisor actor
     * @see net.echinopsii.ariane.community.messaging.common.MomAkkaSupervisor#willStopSoon
     */
    public void preCloseMainSupervisor() {
        MomAkkaSupervisor.willStopSoon(mainSupervisor);
    }

    /**
     * close main supervisor and dead letter logger actor
     */
    public void closeMainSupervisor() {
        this.system.stop(mainSupervisor);
        if (dlLogger!=null) {
            this.system.stop(dlLogger);
            dlLogger = null;
        }
    }

    /**
     * @return this MomClient ID
     */
    @Override
    public String getClientID() {
        return clientID;
    }

    /**
     * set this MomClient ID with provided id
     * @param id
     */
    public void setClientID(String id) {
        this.clientID = id;
    }

    /**
     * @return true if message debug on timeout is enabled else false
     */
    @Override
    public boolean isMsgDebugOnTimeout() {
        return msgDebugOnTimeout;
    }

    /**
     * setup message debug on timeout with provided configuration
     * @param msgDebugOnTimeout
     */
    @Override
    public void setMsgDebugOnTimeout(boolean msgDebugOnTimeout) {
        this.msgDebugOnTimeout = msgDebugOnTimeout;
    }

    /**
     * @return the routees count per service to setup
     */
    public int getRouteesCountPerService() {
        return routeesCountPerService;
    }

    /**
     * setup routees count per service with provided value
     * @param routeesCountPerService
     */
    public void setRouteesCountPerService(int routeesCountPerService) {
        this.routeesCountPerService = routeesCountPerService;
    }

    /**
     * @return rpc timeout (second)
     */
    @Override
    public long getRPCTimout() {
        return this.rpcTimeout;
    }

    /**
     * define rpc timeout with provided value (second)
     * @param rpcTimout
     */
    @Override
    public void setRPCTimout(long rpcTimout) {
        if (rpcTimout > 0) this.rpcTimeout = rpcTimout;
        else this.rpcTimeout = -1L;
    }

    /**
     * @return rpc retry count
     */
    @Override
    public int getRPCRetry() {
        return this.rpcRetry;
    }

    /**
     * define rpc retry count with provided value
     * @param rpcRetry
     */
    @Override
    public void setRPCRetry(int rpcRetry) {
        this.rpcRetry = rpcRetry;
    }

    /**
     * @return service factory attached to this MomClient
     */
    @Override
    public MomServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    /**
     * define service factory with provided value
     * @param serviceFactory
     */
    public void setServiceFactory(MomServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    /**
     * @return request executors attached to this MomClient
     */
    public List<MomRequestExecutor> getRequestExecutors() {
        return requestExecutors;
    }

    /**
     * Open a new message group request based on the current executing thread calling this method
     * @param groupID the message group request ID (must be unique)
     */
    @Override
    public void openMsgGroupRequest(String groupID) {
        Long threadID = Thread.currentThread().getId();
        this.msgGroupThreadRegistry.put(groupID, threadID);
        this.threadMsgGroupRegistry.put(threadID, groupID);
    }

    /**
     * Get the current message group based on the current executing thread calling this method
     * @return message group ID
     */
    @Override
    public String getCurrentMsgGroup() {
        Long threadID = Thread.currentThread().getId();
        return threadMsgGroupRegistry.get(threadID);
    }

    /**
     * Close the current message group based on the current executing thread calling this method
     * @param groupID the message group ID
     */
    @Override
    public void closeMsgGroupRequest(String groupID) {
        Long threadID = this.msgGroupThreadRegistry.get(groupID);
        if (threadID!=null) {
            this.threadMsgGroupRegistry.remove(threadID);
            this.msgGroupThreadRegistry.remove(groupID);
        }
    }

    /**
     * Open message group services for provided message group - defined with unique groupID.
     * Under the hood this will create a new message group supervisor actor in charge of supervising and cleanly
     * close dedicated services to this message group.
     * Then for all existing main services defined through the service factory a new message group service will be
     * created.
     * @param groupID the message group ID
     */
    @Override
    public void openMsgGroupServices(String groupID) {
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

    /**
     * Close message group services for provided message group - defined with unique groupID
     * For all existing main services defined through the service factory, the message group service will be
     * closed.
     * @param groupID the message group ID
     */
    @Override
    public void closeMsgGroupServices(String groupID) {
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

    /**
     * @param groupID the message group ID
     * @return the message group supervisor actor ref dedicated to provided message group ID
     */
    public ActorRef getMsgGroupSupervisor(String groupID) {
        return msgGroupSupervisors.get(groupID);
    }

    /**
     * pre close message group supervisor actors
     * @see net.echinopsii.ariane.community.messaging.common.MomAkkaSupervisor#willStopSoon
     */
    public void preCloseMsgGroupSupervisors() {
        for (ActorRef supervisor : msgGroupSupervisors.values()) MomAkkaSupervisor.willStopSoon(supervisor);
    }

    /**
     * close all message group supervisors
     */
    public void closeMsgGroupSupervisors() {
        for (ActorRef supervisor : msgGroupSupervisors.values()) this.system.stop(supervisor);
    }
}