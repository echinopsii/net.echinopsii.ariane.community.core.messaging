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
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsAppHPMsgSrvWorker;
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.TimeoutException;

import static junit.framework.TestCase.assertTrue;

public class MsgGroupRPCTest {

    private static MomClient client = null;
    private static TestSessionRequestWorker sessionRequestWorker;
    private static TestSessionServiceWorker sessionServiceWorker;
    private static TestRequestWorker requestWorker;
    private static TestReplyWorker   replyWorker;


    @BeforeClass
    public static void testSetup() throws IllegalAccessException, ClassNotFoundException, InstantiationException, IOException {
        Properties props = new Properties();
        props.load(ClientTest.class.getResourceAsStream("/nats-test.properties"));
        client = MomClientFactory.make(props.getProperty(MomClient.MOM_CLI));

        try {
            client.init(props);
            sessionRequestWorker = new TestSessionRequestWorker(client);
            sessionServiceWorker = new TestSessionServiceWorker(client);
            client.getServiceFactory().requestService("SESSION_SUBJECT", sessionServiceWorker);
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

    static class TestSessionServiceWorker implements AppMsgWorker {
        MomClient client ;

        TestSessionServiceWorker(MomClient client) {
            this.client = client;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            Map<String, Object> reply = new HashMap<>(message);
            if (message.get("OP").equals("OPEN_SESSION")) {
                String sessionID = String.valueOf(UUID.randomUUID());
                client.openMsgGroupServices(sessionID);
                reply.put("SESSION_ID", sessionID);
            } else if (message.get("OP").equals("CLOSE_SESSION")) {
                String sessionID = (String) message.get("SESSION_ID");
                client.closeMsgGroupServices(sessionID);
            }
            return reply;
        }
    }

    static class TestSessionRequestWorker implements AppMsgWorker {
        MomClient client ;

        TestSessionRequestWorker(MomClient client) {
            this.client = client;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            return message;
        }
    }

    static class TestRequestWorker extends MomAkkaAbsAppHPMsgSrvWorker {
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
                    OK = !OK;

                reply = new HashMap();
                reply.put(MomMsgTranslator.MSG_BODY, this.msgReplyBody);
            }
            return reply;
        }

        public boolean isOK() {
            return OK;
        }
    }

    static class TestReplyWorker implements AppMsgWorker {
        boolean OK = false;
        byte[] msgBody = null;

        public TestReplyWorker(byte[] msgBody_) {
            this.msgBody = msgBody_;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            byte[] recvMsgBody = (byte [])message.get(MomMsgTranslator.MSG_BODY);
            if (Arrays.equals(recvMsgBody, this.msgBody))
                OK = !OK;
            return message;
        }

        public boolean isOK() {
            return OK;
        }
    }

    private void openSession() throws TimeoutException {
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("OP", "OPEN_SESSION");
        String sessionID = (String) client.createRequestExecutor().RPC(request, "SESSION_SUBJECT", sessionRequestWorker).get("SESSION_ID");
        client.openMsgGroupRequest(sessionID);
    }

    private void closeSession() throws TimeoutException {
        String sessionID = client.getCurrentMsgGroup();
        client.closeMsgGroupRequest(sessionID);
        Map<String, Object> request = new HashMap<String, Object>();
        request.put("OP", "CLOSE_SESSION");
        request.put("SESSION_ID", sessionID);
        client.createRequestExecutor().RPC(request, "SESSION_SUBJECT", sessionRequestWorker);
    }

    @Test
    public void testGroupRPC() throws InterruptedException, TimeoutException {
        if (client!=null) {
            requestWorker = new TestRequestWorker(client.getServiceFactory(), sendedRequestBody.getBytes(), sendedReplyBody.getBytes());
            replyWorker   = new TestReplyWorker(sendedReplyBody.getBytes());

            client.getServiceFactory().msgGroupRequestService("RPC_SUBJECT", requestWorker);

            openSession();

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());

            closeSession();

            request.clear();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT", replyWorker);

            assertTrue(!requestWorker.isOK());
            assertTrue(!replyWorker.isOK());
       }
    }

    @Test
    public void testGroupHPRPC_1() throws InterruptedException, TimeoutException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            requestWorker = new TestRequestWorker(client.getServiceFactory(), highPayloadBody, sendedReplyBody.getBytes());
            replyWorker   = new TestReplyWorker(sendedReplyBody.getBytes());

            client.getServiceFactory().msgGroupRequestService("RPC_SUBJECT_SPLIT_1", requestWorker);

            openSession();

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_1", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());

            closeSession();

            request.clear();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_1", replyWorker);

            assertTrue(!requestWorker.isOK());
            assertTrue(!replyWorker.isOK());
        }
    }

    @Test
    public void testGroupHPRPC_2() throws InterruptedException, TimeoutException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            requestWorker = new TestRequestWorker(client.getServiceFactory(), highPayloadBody, highPayloadBody);
            replyWorker   = new TestReplyWorker(highPayloadBody);

            client.getServiceFactory().msgGroupRequestService("RPC_SUBJECT_SPLIT_2", requestWorker);

            openSession();

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_2", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());

            closeSession();

            request.clear();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_2", replyWorker);

            assertTrue(!requestWorker.isOK());
            assertTrue(!replyWorker.isOK());
        }
    }

    @Test
    public void testGroupHPRPC_3() throws InterruptedException, TimeoutException {
        if (client!=null) {
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }

            requestWorker = new TestRequestWorker(client.getServiceFactory(), sendedRequestBody.getBytes(), highPayloadBody);
            replyWorker   = new TestReplyWorker(highPayloadBody);

            client.getServiceFactory().msgGroupRequestService("RPC_SUBJECT_SPLIT_3", requestWorker);

            openSession();

            Map<String, Object> request = new HashMap<String, Object>();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_3", replyWorker);

            assertTrue(requestWorker.isOK());
            assertTrue(replyWorker.isOK());

            closeSession();

            request.clear();
            request.put("OP", "TEST");
            request.put("ARGS_BOOL", true);
            request.put("ARGS_LONG", 0);
            request.put("ARGS_STRING", "toto");
            request.put(MomMsgTranslator.MSG_BODY, sendedRequestBody);
            client.createRequestExecutor().RPC(request, "RPC_SUBJECT_SPLIT_3", replyWorker);

            assertTrue(!requestWorker.isOK());
            assertTrue(!replyWorker.isOK());
        }
    }
}