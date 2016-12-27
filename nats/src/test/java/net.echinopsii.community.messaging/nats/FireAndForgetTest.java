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
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsAppMsgWorker;
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
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
    final static byte[] highPayloadBody = new byte[2000000];

    class TestMsgWorker extends MomAkkaAbsAppMsgWorker {
        boolean OK = false;
        byte[] msgBody = null;

        public TestMsgWorker(MomServiceFactory serviceFactory, byte[] msgBody_) {
            super(serviceFactory);
            this.msgBody = msgBody_;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            Map<String, Object> ret = super.apply(message);
            if (ret==null) {
                byte[] recvMsgBody = (byte[]) message.get(MomMsgTranslator.MSG_BODY);
                if (Arrays.equals(recvMsgBody, this.msgBody))
                    OK = true;
            }
            return ret;
        }

        public boolean isOK() {
            return OK;
        }
    }

    @Test
    public void testFireAndForget() throws InterruptedException {
        if (client!=null) {
            TestMsgWorker test = new TestMsgWorker(client.getServiceFactory(), sendedMsgBody.getBytes());
            client.getServiceFactory().requestService("FAF_SUBJECT", test);

            Map<String, Object> message = new HashMap<String, Object>();
            message.put(MomMsgTranslator.MSG_BODY, sendedMsgBody);
            client.createRequestExecutor().FAF(message, "FAF_SUBJECT");

            Thread.sleep(1000);
            assertTrue(test.isOK());
        }
    }

    @Test
    public void testHighPayloadFireAndForget() throws InterruptedException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            TestMsgWorker test = new TestMsgWorker(client.getServiceFactory(), highPayloadBody);
            client.getServiceFactory().requestService("FAF_SUBJECT_SPLIT", test);

            Map<String, Object> message = new HashMap<>();
            message.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            //System.out.println(highPayloadBody.length);
            client.createRequestExecutor().FAF(message, "FAF_SUBJECT_SPLIT");

            Thread.sleep(1000);

            assertTrue(test.isOK());
        }
    }
}