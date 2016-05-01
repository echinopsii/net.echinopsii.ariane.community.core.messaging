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

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static junit.framework.TestCase.assertTrue;

public class RPCTest {

    private static MomClient client = null;

    @BeforeClass
    public static void testSetup() throws IllegalAccessException, ClassNotFoundException, InstantiationException {
        Properties props = new Properties();
        props.put(MomClient.MOM_HOST, "localhost");
        props.put(MomClient.MOM_PORT, 4222);

        client = MomClientFactory.make("net.echinopsii.ariane.community.messaging.nats.Client");

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

    final static String sendedRequestBody = "Hello NATS!";
    final static String sendedReplyBody   = "Hello Client!";

    class TestRequestWorker implements AppMsgWorker {
        boolean OK = false;

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            String recvMsgBody = new String((byte [])message.get(MomMsgTranslator.MSG_BODY));
            if (recvMsgBody.equals(sendedRequestBody))
                OK = true;

            Map<String, Object> reply = new HashMap<String, Object>();
            reply.put(MomMsgTranslator.MSG_BODY, sendedReplyBody);

            return reply;
        }

        public boolean isOK() {
            return OK;
        }
    }

    class TestReplyWorker implements AppMsgWorker {
        boolean OK = false;

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            String recvMsgBody = new String((byte [])message.get(MomMsgTranslator.MSG_BODY));
            if (recvMsgBody.equals(sendedReplyBody))
                OK = true;
            return message;
        }

        public boolean isOK() {
            return OK;
        }
    }

    @Test
    public void testRPC() throws InterruptedException {
        if (client!=null) {
            TestRequestWorker requestWorker = new TestRequestWorker();
            TestReplyWorker   replyWorker   = new TestReplyWorker();

            client.getServiceFactory().requestService("RPC_SUBJECT", requestWorker);

            Map<String, Object> request = new HashMap<String, Object>();
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());
        }
    }

}