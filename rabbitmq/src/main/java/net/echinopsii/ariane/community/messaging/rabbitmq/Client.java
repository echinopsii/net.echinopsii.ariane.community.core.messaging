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

package net.echinopsii.ariane.community.messaging.rabbitmq;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.api.MomService;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class Client implements MomClient {

    public static final String PROP_PRODUCT_KEY = "mom_cli.rabbitmq.product";
    public static final String RBQ_PRODUCT_KEY  = "product";

    public static final String PROP_INFORMATION_KEY = "mom_cli.rabbitmq.information";
    public static final String RBQ_INFORMATION_KEY  = "information";

    public static final String PROP_PLATFORM_KEY = "mom_cli.rabbitmq.platform";
    public static final String RBQ_PLATFORM_KEY  = "platform";

    public static final String PROP_COPYRIGHT_KEY = "mom_cli.rabbitmq.copyright";
    public static final String RBQ_COPYRIGHT_KEY  = "copyright";

    public static final String PROP_VERSION_KEY = "mom_cli.rabbitmq.version";
    public static final String RBQ_VERSION_KEY  = "version";

    public static final String PROP_ARIANE_KEYS  = "ariane";

    private MomServiceFactory serviceFactory ;
    private List<MomRequestExecutor> requestExecutors = new ArrayList<MomRequestExecutor>();

    private ActorSystem       system     = ActorSystem.create("MySystem");
    private String            clientID   = null;
    private ConnectionFactory factory    = null;
    private Connection        connection = null;

    @Override
    public void init(Properties properties) throws IOException {
        factory = new ConnectionFactory();
        factory.setHost((String) properties.get(MOM_HOST));
        factory.setPort(new Integer((String)properties.get(MOM_PORT)));

        Map<String, Object> factoryProperties = factory.getClientProperties();
        if (properties.getProperty(PROP_PRODUCT_KEY)!=null)
            factoryProperties.put(RBQ_PRODUCT_KEY,properties.getProperty(PROP_PRODUCT_KEY));
        if (properties.getProperty(PROP_INFORMATION_KEY)!=null) {
            clientID = properties.getProperty(PROP_INFORMATION_KEY);
            factoryProperties.put(RBQ_INFORMATION_KEY, clientID);
        } else {
            clientID = (String)properties.get(RBQ_INFORMATION_KEY);
        }
        if (properties.getProperty(PROP_PLATFORM_KEY)!=null)
            factoryProperties.put(RBQ_PLATFORM_KEY,properties.getProperty(PROP_PLATFORM_KEY));
        else
            factoryProperties.put(RBQ_PLATFORM_KEY, "Java " + System.getProperty("java.version"));
        if (properties.getProperty(PROP_COPYRIGHT_KEY)!=null)
            factoryProperties.put(RBQ_COPYRIGHT_KEY, properties.getProperty(PROP_COPYRIGHT_KEY));
        if (properties.getProperty(PROP_VERSION_KEY)!=null)
            factoryProperties.put(RBQ_VERSION_KEY, properties.getProperty(PROP_VERSION_KEY));

        for (Object key : properties.keySet())
            if (key instanceof String && ((String)key).startsWith(PROP_ARIANE_KEYS))
                factoryProperties.put((String)key, properties.getProperty((String)key));

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