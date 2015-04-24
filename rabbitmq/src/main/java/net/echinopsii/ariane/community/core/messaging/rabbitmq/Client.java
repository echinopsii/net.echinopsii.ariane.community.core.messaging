/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
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

package net.echinopsii.ariane.community.core.messaging.rabbitmq;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.echinopsii.ariane.community.core.messaging.api.MomClient;
import net.echinopsii.ariane.community.core.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.core.messaging.api.MomService;
import net.echinopsii.ariane.community.core.messaging.api.MomServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

public class Client implements MomClient {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    public static final String RBQ_PRODUCT_KEY     = "product";
    public static final String RBQ_INFORMATION_KEY = "information";
    public static final String RBQ_PLATFORM_KEY    = "platform";
    public static final String RBQ_COPYRIGHT_KEY   = "copyright";
    public static final String RBQ_VERSION_KEY     = "version";

    private MomServiceFactory serviceFactory ;
    private List<MomRequestExecutor> requestExecutors = new ArrayList<MomRequestExecutor>();

    private ActorSystem       system     = null;
    private String            clientID   = null;
    private ConnectionFactory factory    = null;
    private Connection        connection = null;

    @Override
    public void init(Properties properties) throws Exception {
        Dictionary props = new Properties();
        for(Object key : properties.keySet())
            props.put(key, properties.get(key));
        init(props);
    }

    @Override
    public void init(Dictionary properties) throws Exception {
        system = MessagingAkkaSystemActivator.getSystem();
        factory = new ConnectionFactory();
        factory.setHost((String) properties.get(MOM_HOST));
        factory.setPort(new Integer((String)properties.get(MOM_PORT)));
        if (properties.get(RBQ_VHOST)!=null)
            factory.setVirtualHost((String)properties.get(RBQ_VHOST));
        if (properties.get(MOM_USER)!=null)
            factory.setUsername((String)properties.get(MOM_USER));
        if (properties.get(MOM_PSWD)!=null)
            factory.setPassword((String)properties.get(MOM_PSWD));

        Map<String, Object> factoryProperties = factory.getClientProperties();
        if (properties.get(MomClient.RBQ_PRODUCT_KEY)!=null)
            factoryProperties.put(RBQ_PRODUCT_KEY,properties.get(MomClient.RBQ_PRODUCT_KEY));
        if (properties.get(MomClient.RBQ_INFORMATION_KEY)!=null) {
            clientID = (String)properties.get(MomClient.RBQ_INFORMATION_KEY);
            factoryProperties.put(RBQ_INFORMATION_KEY, clientID);
        } else {
            clientID = (String)properties.get(RBQ_INFORMATION_KEY);
        }
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

        serviceFactory = new ServiceFactory(this);
    }

    @Override
    public void close() throws IOException {
        for (MomRequestExecutor rexec : requestExecutors)
            ((RequestExecutor)rexec).stop();
        if (serviceFactory!=null)
            for (MomService<ActorRef> service : ((ServiceFactory)serviceFactory).getServices())
                service.stop();
        if (connection.isOpen())
            connection.close();
    }

    @Override
    public boolean isConnected() {
        return connection.isOpen();
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public MomRequestExecutor createRequestExecutor() {
        MomRequestExecutor ret = null;
        try {
            ret = new RequestExecutor(this);
            requestExecutors.add(ret);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

    @Override
    public MomServiceFactory getServiceFactory() {
        return serviceFactory;
    }

    @Override
    public String getClientID() {
        return clientID;
    }

    public ActorSystem getActorSystem() {
        return system;
    }
}