/**
 * Messaging NATS Impl test
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

package net.echinopsii.community.messaging.nats;

import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

public class ClientTest {

    private static MomClient client = null;

    @BeforeClass
    public static void testSetup() throws IllegalAccessException, ClassNotFoundException, InstantiationException, IOException {
        Properties props = new Properties();
        props.load(ClientTest.class.getResourceAsStream("/nats-test.properties"));
        //props.put(MomClient.MOM_HOST, "localhost");
        //props.put(MomClient.MOM_PORT, "4222");
        //props.put(MomClient.MOM_USER, "ariane");
        //props.put(MomClient.MOM_PSWD, "password");
        //props.put(MomClient.NATS_CONNECTION_NAME, "ClientTest");
        //props.put("ariane.pgurl", "jmx://frontoffice-01.lab01.dev.dekatonshivr.echinopsii.net:9010");
        //props.put("ariane.osi", "frontoffice-01.lab01.dev.dekatonshivr.echinopsii.net");
        //props.put("ariane.otm", "FrontOffice OPS Team");
        //props.put("ariane.dtm", "FrontOffice DEV Team");
        client = MomClientFactory.make(props.getProperty(MomClient.MOM_CLI));

        try {
            client.init(props);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("No local NATS to test");
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

    public static MomClient getClient() {
        return client;
    }
}