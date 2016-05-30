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
import net.echinopsii.ariane.community.messaging.common.MomClientFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import static junit.framework.TestCase.assertTrue;

public class TopicTest {
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

    class TestSubscriber implements AppMsgWorker {

        private int msgNumber = 0;

        @Override
        public Map<String, Object> apply(Map<String, Object> message) {
            msgNumber++;
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

            TestSubscriber subsAll    = new TestSubscriber();
            TestSubscriber subsStockA = new TestSubscriber();
            TestSubscriber subsStockB = new TestSubscriber();
            TestSubscriber subsStockC = new TestSubscriber();

            MomService subsService  = client.getServiceFactory().subscriberService("PRICE", null, subsAll);
            MomService subsServiceA = client.getServiceFactory().subscriberService("PRICE", "STOCKA", subsStockA);
            MomService subsServiceB = client.getServiceFactory().subscriberService("PRICE", "STOCKB", subsStockB);
            MomService subsServiceC = client.getServiceFactory().subscriberService("PRICE", "STOCKC", subsStockC);

            MomService feedServiceA = client.getServiceFactory().feederService("PRICE", "STOCKA", feederStockA.getInterval(), feederStockA);
            MomService feedServiceB = client.getServiceFactory().feederService("PRICE", "STOCKB", feederStockB.getInterval(), feederStockB);
            MomService feedServiceC = client.getServiceFactory().feederService("PRICE", "STOCKC", feederStockC.getInterval(), feederStockC);

            while(feederStockA.getMsgNumber()<=10)
                Thread.sleep(feederStockA.getInterval());

            //assertTrue(subsAll.getMsgNumber()==(feederStockA.getMsgNumber()+feederStockB.getMsgNumber()+feederStockC.getMsgNumber()));
            assertTrue(subsStockA.getMsgNumber()==feederStockA.getMsgNumber());
            assertTrue(subsStockB.getMsgNumber()==feederStockB.getMsgNumber());
            assertTrue(subsStockC.getMsgNumber()==feederStockC.getMsgNumber());
        }
    }
}