/**
 * [DEFINE YOUR PROJECT NAME/MODULE HERE]
 * [DEFINE YOUR PROJECT DESCRIPTION HERE] 
 * Copyright (C) 29/08/14 echinopsii
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

import net.echinopsii.ariane.community.messaging.api.*;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsAppMsgWorker;
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
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

public class TopicTest {
    private static MomClient client = null;
    private static byte[] highPayloadBody = new byte[2000000];

    @BeforeClass
    public static void testSetup() throws IllegalAccessException, ClassNotFoundException, InstantiationException, IOException {
        Properties props = new Properties();
        props.load(ClientTest.class.getResourceAsStream("/nats-test.properties"));
        client = MomClientFactory.make(props.getProperty(MomClient.MOM_CLI));

        try {
            client.init(props);
            for (int i=0; i < highPayloadBody.length; i+=4) {
                byte[] intBytes = ByteBuffer.allocate(4).putInt(i).array();
                for (int j=0; j < 4; j++) highPayloadBody[i+j] = intBytes[j];
            }
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

    class TestFeeder implements AppMsgFeeder {

        private int interval = 100;
        private String stockName;
        private int msgNumber = 0;

        public TestFeeder(String sname) {
            stockName = sname;
        }

        @Override
        public Map<String, Object> apply() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("NAME", stockName);
            int price = (int)(Math.random() * 10 + Math.random() * 100 + Math.random() * 1000);
            ret.put("PRICE", price );
            msgNumber++;
            return ret;
        }

        @Override
        public int getInterval() {
            return interval;
        }

        public int getMsgNumber() {
            return msgNumber;
        }
    }

    class TestHPFeeder implements AppMsgFeeder {

        private int interval = 100;
        private String stockName;
        private int msgNumber = 0;

        public TestHPFeeder(String sname) {
            stockName = sname;
        }

        @Override
        public Map<String, Object> apply() {
            Map<String, Object> ret = new HashMap<String, Object>();
            ret.put("NAME", stockName);
            int price = (int)(Math.random() * 10 + Math.random() * 100 + Math.random() * 1000);
            ret.put("PRICE", price );
            ret.put(MomMsgTranslator.MSG_BODY, highPayloadBody);
            msgNumber++;
            return ret;
        }

        @Override
        public int getInterval() {
            return interval;
        }

        public int getMsgNumber() {
            return msgNumber;
        }
    }

    class TestSubscriber extends MomAkkaAbsAppMsgWorker {

        private byte[] awaitedBody = null;
        private int msgNumber = 0;

        public TestSubscriber(MomServiceFactory serviceFactory, byte[] awaitedBody) {
            super(serviceFactory);
            this.awaitedBody = awaitedBody;
        }

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            if (message!=null)
                if (this.awaitedBody==null || Arrays.equals(this.awaitedBody, (byte [])message.get(MomMsgTranslator.MSG_BODY)))
                    msgNumber++;
                else System.out.println("Problem with awaited body!");
            return message;
        }

        public int getMsgNumber() {
            return msgNumber;
        }
    }

    @Test
    public void testPubSubTopic() throws InterruptedException {
        if (client!=null) {
            TestFeeder feederStockA = new TestFeeder("STOCKA");
            TestFeeder feederStockB = new TestFeeder("STOCKB");
            TestFeeder feederStockC = new TestFeeder("STOCKC");

            TestSubscriber subsAll    = new TestSubscriber(client.getServiceFactory(), null);
            TestSubscriber subsStockA = new TestSubscriber(client.getServiceFactory(), null);
            TestSubscriber subsStockB = new TestSubscriber(client.getServiceFactory(), null);
            TestSubscriber subsStockC = new TestSubscriber(client.getServiceFactory(), null);

            MomService subsService  = client.getServiceFactory().subscriberService("PRICE", null, subsAll);
            MomService subsServiceA = client.getServiceFactory().subscriberService("PRICE", "STOCKA", subsStockA);
            MomService subsServiceB = client.getServiceFactory().subscriberService("PRICE", "STOCKB", subsStockB);
            MomService subsServiceC = client.getServiceFactory().subscriberService("PRICE", "STOCKC", subsStockC);

            MomService feedServiceA = client.getServiceFactory().feederService("PRICE", "STOCKA", feederStockA.getInterval(), feederStockA);
            MomService feedServiceB = client.getServiceFactory().feederService("PRICE", "STOCKB", feederStockB.getInterval(), feederStockB);
            MomService feedServiceC = client.getServiceFactory().feederService("PRICE", "STOCKC", feederStockC.getInterval(), feederStockC);

            while(feederStockA.getMsgNumber()<=10)
                Thread.sleep(feederStockA.getInterval());

            feedServiceA.stop();
            feedServiceB.stop();
            feedServiceC.stop();

            Thread.sleep(feederStockA.getInterval());

            subsService.stop();
            subsServiceA.stop();
            subsServiceB.stop();
            subsServiceC.stop();

            assertTrue(subsAll.getMsgNumber() == (feederStockA.getMsgNumber() + feederStockB.getMsgNumber() + feederStockC.getMsgNumber()));
            assertTrue(subsStockA.getMsgNumber() == feederStockA.getMsgNumber());
            assertTrue(subsStockB.getMsgNumber()==feederStockB.getMsgNumber());
            assertTrue(subsStockC.getMsgNumber()==feederStockC.getMsgNumber());

        }
    }


    @Test
    public void testHPPubSubTopic() throws InterruptedException {
        if (client!=null) {
            TestHPFeeder feederStockA = new TestHPFeeder("HPSTOCKA");
            TestHPFeeder feederStockB = new TestHPFeeder("HPSTOCKB");
            TestHPFeeder feederStockC = new TestHPFeeder("HPSTOCKC");

            TestSubscriber subsStockA = new TestSubscriber(client.getServiceFactory(), highPayloadBody);
            TestSubscriber subsStockB = new TestSubscriber(client.getServiceFactory(), highPayloadBody);
            TestSubscriber subsStockC = new TestSubscriber(client.getServiceFactory(), highPayloadBody);

            MomService subsServiceA = client.getServiceFactory().subscriberService("PRICE", "STOCKA", subsStockA);
            MomService subsServiceB = client.getServiceFactory().subscriberService("PRICE", "STOCKB", subsStockB);
            MomService subsServiceC = client.getServiceFactory().subscriberService("PRICE", "STOCKC", subsStockC);

            MomService feedServiceA = client.getServiceFactory().feederService("PRICE", "STOCKA", feederStockA.getInterval(), feederStockA);
            MomService feedServiceB = client.getServiceFactory().feederService("PRICE", "STOCKB", feederStockB.getInterval(), feederStockB);
            MomService feedServiceC = client.getServiceFactory().feederService("PRICE", "STOCKC", feederStockC.getInterval(), feederStockC);

            while(feederStockA.getMsgNumber()<=10)
                Thread.sleep(feederStockA.getInterval());

            feedServiceA.stop();
            feedServiceB.stop();
            feedServiceC.stop();

            Thread.sleep(feederStockA.getInterval()*5);

            subsServiceA.stop();
            subsServiceB.stop();
            subsServiceC.stop();

            assertTrue(subsStockA.getMsgNumber()==feederStockA.getMsgNumber());
            assertTrue(subsStockB.getMsgNumber()==feederStockB.getMsgNumber());
            assertTrue(subsStockC.getMsgNumber()==feederStockC.getMsgNumber());
        }
    }
}