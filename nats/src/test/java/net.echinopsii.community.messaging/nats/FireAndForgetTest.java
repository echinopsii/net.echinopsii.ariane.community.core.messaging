/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
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

import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static junit.framework.TestCase.assertTrue;

public class FireAndForgetTest {

    private static MomClient client = null;

    @BeforeClass
    public static void testSetup() throws IllegalAccessException, ClassNotFoundException, InstantiationException, IOException {
        Properties props = new Properties();
        props.load(ClientTest.class.getResourceAsStream("/nats-test.properties"));
        client = MomClientFactory.make(props.getProperty(MomClient.MOM_CLI));

        try {
            client.init(props);
        } catch (Exception e) {
            System.err.println("No local NATS to test");
            client = null;
        }
    }

    @AfterClass
    public static void testCleanup() throws Exception {
        if (client!=null)
            client.close();
    }

    final static String sendedMsgBody = "Hello NATS";

    class TestMsgWorker implements AppMsgWorker {

        boolean OK = false;

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            String recvMsgBody = new String((byte [])message.get(MomMsgTranslator.MSG_BODY));
            if (recvMsgBody.equals(sendedMsgBody))
                OK = true;
            return null;
        }

        public boolean isOK() {
            return OK;
        }
    }

    @Test
    public void testFireAndForget() throws InterruptedException {
        if (client!=null) {
            TestMsgWorker test = new TestMsgWorker();

            client.getServiceFactory().requestService("FAF_SUBJECT", test);

            Map<String, Object> message = new HashMap<String, Object>();
            message.put(MomMsgTranslator.MSG_BODY, sendedMsgBody);
            client.createRequestExecutor().FAF(message, "FAF_SUBJECT");

            Thread.sleep(1000);

            assertTrue(test.isOK());
        }
    }

}