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
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertTrue;

public class RPCTest {

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

    final static String sendedRequestBody = "Hello NATS!";
    final static String sendedReplyBody   = "Hello Client!";
    final static byte[] highPayloadBody = new byte[2000000];

    class TestRequestWorker extends MomAkkaAbsAppMsgWorker {
        boolean OK = false;
        byte[] msgRequestBody = null;
        byte[] msgReplyBody = null;

        public TestRequestWorker(MomServiceFactory serviceFactory, byte[] msgRequestBody_, byte[] msgReplyBody_) {
            super(serviceFactory);
            this.msgRequestBody = msgRequestBody_;
            this.msgReplyBody = msgReplyBody_;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            Map<String, Object> reply = super.apply(message);
            if (reply==null) {
                byte[] recvMsgBody = (byte[]) message.get(MomMsgTranslator.MSG_BODY);
                if (Arrays.equals(recvMsgBody, this.msgRequestBody))
                    OK = true;

                reply = new HashMap();
                reply.put(MomMsgTranslator.MSG_BODY, this.msgReplyBody);
            }
            return reply;
        }

        public boolean isOK() {
            return OK;
        }
    }

    class TestReplyWorker implements AppMsgWorker {
        boolean OK = false;
        byte[] msgBody = null;

        public TestReplyWorker(byte[] msgBody_) {
            this.msgBody = msgBody_;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            byte[] recvMsgBody = (byte [])message.get(MomMsgTranslator.MSG_BODY);
            if (Arrays.equals(recvMsgBody, this.msgBody))
                OK = true;
            return message;
        }

        public boolean isOK() {
            return OK;
        }
    }

    @Test
    public void testRPC() throws InterruptedException, TimeoutException {
        if (client!=null) {
            TestRequestWorker requestWorker = new TestRequestWorker(client.getServiceFactory(), sendedRequestBody.getBytes(), sendedReplyBody.getBytes());
            TestReplyWorker   replyWorker   = new TestReplyWorker(sendedReplyBody.getBytes());

            client.getServiceFactory().requestService("RPC_SUBJECT", requestWorker);

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());
        }
    }

    @Test
    public void testHighPayloadRPC_1() throws InterruptedException, TimeoutException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            TestRequestWorker requestWorker = new TestRequestWorker(client.getServiceFactory(), highPayloadBody, sendedReplyBody.getBytes());
            TestReplyWorker   replyWorker   = new TestReplyWorker(sendedReplyBody.getBytes());

            client.getServiceFactory().requestService("RPC_SUBJECT_SPLIT_1", requestWorker);

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_1", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());
        }
    }

    @Test
    public void testHighPayloadRPC_2() throws InterruptedException, TimeoutException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            TestRequestWorker requestWorker = new TestRequestWorker(client.getServiceFactory(), highPayloadBody, highPayloadBody);
            TestReplyWorker   replyWorker   = new TestReplyWorker(highPayloadBody);

            client.getServiceFactory().requestService("RPC_SUBJECT_SPLIT_2", requestWorker);

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_2", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());
        }
    }

    @Test
    public void testHighPayloadRPC_3() throws InterruptedException, TimeoutException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            TestRequestWorker requestWorker = new TestRequestWorker(client.getServiceFactory(), sendedRequestBody.getBytes(), highPayloadBody);
            TestReplyWorker   replyWorker   = new TestReplyWorker(highPayloadBody);

            client.getServiceFactory().requestService("RPC_SUBJECT_SPLIT_3", requestWorker);

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_3", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());
        }
    }

}