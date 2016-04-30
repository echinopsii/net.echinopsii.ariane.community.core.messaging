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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Dictionary;

public class Client extends MomAkkaAbsClient implements MomClient {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    private Connection connection = null;

    @Override
    public void init(Dictionary properties) throws Exception {
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
        if (properties.get(MOM_USER)!=null)
            factory.setUsername((String) properties.get(MOM_USER));
        if (properties.get(MOM_PSWD)!=null)
            factory.setPassword((String) properties.get(MOM_PSWD));

        connection = factory.createConnection();

        super.setServiceFactory(new ServiceFactory(this));
    }

    @Override
    public void close() throws Exception {
        for (MomRequestExecutor rexec : super.getRequestExecutors())
            ((RequestExecutor)rexec).stop();
        if (super.getServiceFactory()!=null)
            for (MomService<ActorRef> service : ((ServiceFactory)super.getServiceFactory()).getServices())
                service.stop();
        if (!connection.isClosed())
            connection.close();
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public boolean isConnected() {
        return !connection.isClosed();
    }

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
}
