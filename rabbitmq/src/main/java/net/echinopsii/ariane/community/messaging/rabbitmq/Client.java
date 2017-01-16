/**
 * Messaging - RabbitMQ Implementation
 * Client implementation
 * Copyright (C) 8/24/14 echinopsii
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
import akka.actor.ActorSystem;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.api.MomService;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 * Client class implementing {@link net.echinopsii.ariane.community.messaging.api.MomClient} interface for RabbitMQ MoM
 */
public class Client extends MomAkkaAbsClient implements MomClient {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static final String RBQ_PRODUCT_KEY     = "product";
    public static final String RBQ_INFORMATION_KEY = "information";
    public static final String RBQ_PLATFORM_KEY    = "platform";
    public static final String RBQ_COPYRIGHT_KEY   = "copyright";
    public static final String RBQ_VERSION_KEY     = "version";

    private Connection        connection = null;

    /**
     * Initialize RabbitMQ connection with provided properties and this client ServiceFactory.
     * <br/>
     * Following properties fields MUST be defined :
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_HOST}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_PORT}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#RBQ_VHOST}
     * <br/>
     * Following properties fields MAY be defined:
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_USER}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_PSWD}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_MSG_DEBUG_ON_TIMEOUT}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_ROUTEES_NB_PER_SERVICE}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_RPC_TIMEOUT}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#MOM_CLI_RPC_RETRY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#RBQ_INFORMATION_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#RBQ_PRODUCT_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#RBQ_PLATFORM_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#RBQ_COPYRIGHT_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#RBQ_VERSION_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#ARIANE_APP_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#ARIANE_CMP_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#ARIANE_OSI_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#ARIANE_OTM_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#ARIANE_PGURL_KEY}
     * {@link net.echinopsii.ariane.community.messaging.api.MomClient#ARIANE_PID_KEY}
     * @param properties configuration properties
     * @throws IOException if problems to join NATS server
     */
    @Override
    public void init(Dictionary properties) throws IOException {
        if (properties.get(MomClient.RBQ_INFORMATION_KEY)!=null)
            super.setClientID((String) properties.get(MomClient.RBQ_INFORMATION_KEY));
        if (properties.get(MOM_CLI_MSG_DEBUG_ON_TIMEOUT)!=null &&
                (((String)properties.get(MOM_CLI_MSG_DEBUG_ON_TIMEOUT)).toLowerCase().equals("true")))
            super.setMsgDebugOnTimeout(true);
        if (properties.get(MOM_CLI_ROUTEES_NB_PER_SERVICE)!=null)
            super.setRouteesCountPerService(new Integer((String) properties.get(MOM_CLI_ROUTEES_NB_PER_SERVICE)));
        try {
            if (Class.forName("akka.osgi.ActorSystemActivator")!=null && MessagingAkkaSystemActivator.getSystem() != null)
                super.setActorSystem(MessagingAkkaSystemActivator.getSystem());
            else
                super.setActorSystem(ActorSystem.create("MySystem"));
        } catch (ClassNotFoundException e) {
            super.setActorSystem(ActorSystem.create("MySystem"));
        }

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost((String) properties.get(MOM_HOST));
        factory.setPort(new Integer((String) properties.get(MOM_PORT)));
        if (properties.get(RBQ_VHOST)!=null)
            factory.setVirtualHost((String) properties.get(RBQ_VHOST));
        if (properties.get(MOM_USER)!=null)
            factory.setUsername((String) properties.get(MOM_USER));
        if (properties.get(MOM_PSWD)!=null)
            factory.setPassword((String) properties.get(MOM_PSWD));

        Map<String, Object> factoryProperties = factory.getClientProperties();
        if (properties.get(MomClient.RBQ_PRODUCT_KEY)!=null)
            factoryProperties.put(RBQ_PRODUCT_KEY,properties.get(MomClient.RBQ_PRODUCT_KEY));
        if (properties.get(MomClient.RBQ_INFORMATION_KEY)!=null)
            factoryProperties.put(RBQ_INFORMATION_KEY, super.getClientID());
        if (properties.get(MomClient.RBQ_PLATFORM_KEY)!=null)
            factoryProperties.put(RBQ_PLATFORM_KEY,properties.get(MomClient.RBQ_PLATFORM_KEY));
        else
            factoryProperties.put(RBQ_PLATFORM_KEY, "Java " + System.getProperty("java.version"));
        if (properties.get(MomClient.RBQ_COPYRIGHT_KEY)!=null)
            factoryProperties.put(RBQ_COPYRIGHT_KEY, properties.get(MomClient.RBQ_COPYRIGHT_KEY));
        if (properties.get(MomClient.RBQ_VERSION_KEY)!=null)
            factoryProperties.put(RBQ_VERSION_KEY, properties.get(MomClient.RBQ_VERSION_KEY));

        Enumeration keys = properties.keys();
        while(keys.hasMoreElements()) {
            Object key = keys.nextElement();
            if (key instanceof String && ((String)key).startsWith(MomClient.ARIANE_KEYS))
                factoryProperties.put((String)key, properties.get((String)key));
        }

        connection = factory.newConnection();

        super.setServiceFactory(new ServiceFactory(this));
    }

    /**
     * Close this client. This will stop any underlying RequestExecutor, Services and Akka supervisors.
     * It will finally close the RabbitMQ connection
     * @throws Exception
     */
    @Override
    public void close() throws IOException {
        for (MomRequestExecutor rexec : super.getRequestExecutors())
            ((RequestExecutor)rexec).stop();
        super.preCloseMsgGroupSupervisors();
        super.preCloseMainSupervisor();
        if (super.getServiceFactory()!=null)
            for (MomService<ActorRef> service : ((ServiceFactory)super.getServiceFactory()).getServices())
                service.stop();
        super.closeMsgGroupSupervisors();
        super.closeMainSupervisor();
        if (connection.isOpen())
            connection.close();
    }

    /**
     * @return true if connected on configured RabbitMQ broker else false
     */
    @Override
    public boolean isConnected() {
        return connection.isOpen();
    }

    /**
     * @return the client RabbitMQ connection
     */
    @Override
    public Connection getConnection() {
        return connection;
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
}