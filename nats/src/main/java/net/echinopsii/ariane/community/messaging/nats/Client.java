/**
 * Messaging - NATS Implementation
 * Client implementation
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
package net.echinopsii.ariane.community.messaging.nats;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import io.nats.client.Connection;
import io.nats.client.ConnectionFactory;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.api.MomService;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsClient;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.Dictionary;

/**
 * Client class implementing {@link net.echinopsii.ariane.community.messaging.api.MomClient} interface for NATS MoM
 */
public class Client extends MomAkkaAbsClient implements MomClient {

    private static final Logger log = MomLoggerFactory.getLogger(Client.class);

    private Connection connection = null;
    private ConnectionFactory factory = null;

    /**
     * Initialize NATS connection with provided properties and this client ServiceFactory.
     * <br/>
     * Following properties fields MUST be defined :
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_HOST}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_PORT}
     * <br/>
     * Following properties fields MAY be defined:
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_USER}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_PSWD}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_MSG_DEBUG_ON_TIMEOUT}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_ROUTEES_NB_PER_SERVICE}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_RPC_TIMEOUT}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_RPC_RETRY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#NATS_CONNECTION_NAME}
     * @param properties
     * @throws Exception
     */
    @Override
    public void init(Dictionary properties) throws Exception {
        if (properties.get(NATS_CONNECTION_NAME)!=null)
            super.setClientID((String) properties.get(NATS_CONNECTION_NAME));
        if (properties.get(MOM_CLI_MSG_DEBUG_ON_TIMEOUT)!=null &&
                (((String)properties.get(MOM_CLI_MSG_DEBUG_ON_TIMEOUT)).toLowerCase().equals("true")))
            super.setMsgDebugOnTimeout(true);
        if (properties.get(MOM_CLI_ROUTEES_NB_PER_SERVICE)!=null)
            super.setRouteesCountPerService(new Integer((String) properties.get(MOM_CLI_ROUTEES_NB_PER_SERVICE)));
        if (properties.get(MOM_CLI_RPC_TIMEOUT)!=null)
            super.setRPCTimout(new Long((String) properties.get(MOM_CLI_RPC_TIMEOUT)));
        if (properties.get(MOM_CLI_RPC_RETRY)!=null)
            super.setRPCRetry(new Integer((String) properties.get(MOM_CLI_RPC_RETRY)));
        try {
            if (Class.forName("akka.osgi.ActorSystemActivator")!=null && MessagingAkkaSystemActivator.getSystem()!=null)
                super.setActorSystem(MessagingAkkaSystemActivator.getSystem());
            else
                super.setActorSystem(ActorSystem.create("MySystem"));
        } catch (ClassNotFoundException e) {
            super.setActorSystem(ActorSystem.create("MySystem"));
        }

        factory = new ConnectionFactory();
        factory.setHost((String) properties.get(MOM_HOST));
        factory.setPort(new Integer((String) properties.get(MOM_PORT)));
        if (properties.get(MOM_USER)!=null)
            factory.setUsername((String) properties.get(MOM_USER));
        if (properties.get(MOM_PSWD)!=null)
            factory.setPassword((String) properties.get(MOM_PSWD));
        if (properties.get(NATS_CONNECTION_NAME)!=null)
            factory.setConnectionName((String) properties.get(NATS_CONNECTION_NAME));
        connection = factory.createConnection();
        MsgTranslator.setMsgMaxSize(connection.getMaxPayload());

        super.setServiceFactory(new ServiceFactory(this));
    }

    /**
     * Close this client. This will stop any underlying RequestExecutor, Services and Akka supervisors.
     * It will finally close the NATS connection
     * @throws Exception
     */
    @Override
    public void close() throws Exception {
        for (MomRequestExecutor rexec : super.getRequestExecutors())
            ((RequestExecutor)rexec).stop();
        super.preCloseMsgGroupSupervisors();
        super.preCloseMainSupervisor();
        if (super.getServiceFactory()!=null)
            for (MomService<ActorRef> service : ((ServiceFactory)super.getServiceFactory()).getServices())
                service.stop();
        super.closeMsgGroupSupervisors();
        super.closeMainSupervisor();
        if (!connection.isClosed())
            connection.close();
    }

    /**
     *
     * @return the client NATS connection
     */
    @Override
    public Connection getConnection() {
        return connection;
    }

    /**
     *
     * @return true if connected on configured NATS broker
     */
    @Override
    public boolean isConnected() {
        return !connection.isClosed();
    }

    /**
     * Create a new RequestExecutor and add it into the client request executors registry
     * @return the fresh new created MomRequestExecutor
     */
    @Override
    public MomRequestExecutor createRequestExecutor() {
        MomRequestExecutor ret = null;
        try {
            ret = new RequestExecutor(this);
            super.getRequestExecutors().add(ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Close the message group ID and any resources attached to it.
     * @param groupID the message group ID
     */
    @Override
    public void closeMsgGroupRequest(String groupID) {
        for (MomRequestExecutor requestExecutor : super.getRequestExecutors())
            ((RequestExecutor) requestExecutor).cleanGroupReqResources(groupID);
        super.closeMsgGroupRequest(groupID);
    }

    /**
     * (internal usage)
     * @return the NATS connection factory
     */
    public ConnectionFactory getFactory() {
        return factory;
    }
}
