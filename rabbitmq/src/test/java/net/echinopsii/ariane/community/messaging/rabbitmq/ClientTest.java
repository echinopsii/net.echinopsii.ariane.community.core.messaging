/**
 * Messaging RabbitMQ Impl test
 * Client test
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

import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class ClientTest {

    private static MomClient client = null;

    @BeforeClass
    public static void testSetup() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Properties props = new Properties();
        props.put(MomClient.MOM_HOST, "localhost");
        props.put(MomClient.MOM_PORT, "5672");
        props.put(MomClient.MOM_USER, "ariane");
        props.put(MomClient.MOM_PSWD, "password");
        props.put(MomClient.RBQ_VHOST, "/ariane");
        props.put("ariane.pgurl", "jmx://frontoffice-01.lab01.dev.dekatonshivr.echinopsii.net:9010");
        props.put("ariane.osi", "frontoffice-01.lab01.dev.dekatonshivr.echinopsii.net");
        props.put("ariane.otm", "FrontOffice OPS Team");
        props.put("ariane.dtm", "FrontOffice DEV Team");

        client = MomClientFactory.make("net.echinopsii.ariane.community.messaging.rabbitmq.Client");

        try {
            client.init(props);
        } catch (Exception e) {
            System.err.println("No local rabbit to test");
            client = null;
        }
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (client!=null)
            client.close();
    }

    @Test
    public void testConnection() {
        if (client!=null) {
            assertTrue(client.isConnected());
            assertNotNull(client.getConnection());
            assertNotNull(client.createRequestExecutor());
            assertNotNull(client.getServiceFactory());
        }
    }
}