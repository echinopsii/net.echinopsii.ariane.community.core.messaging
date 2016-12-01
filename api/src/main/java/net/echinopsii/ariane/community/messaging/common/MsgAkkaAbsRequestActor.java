/**
 * Messaging - Common Implementation
 * Message Request Actor Abstract Implementation
 * Copyright (C) 04/30/16 echinopsii
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

package net.echinopsii.ariane.community.messaging.common;

import akka.actor.UntypedActor;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import org.slf4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public abstract class MsgAkkaAbsRequestActor extends UntypedActor {

    private static final Logger log = MomLoggerFactory.getLogger(MsgAkkaAbsRequestActor.class);

    private MomMsgTranslator translator = null;
    private AppMsgWorker msgWorker   = null;
    private MomClient client      = null;

    private boolean isRunning;

    private long replyCacheRetentionTime = 20*1000; // default 20 sec.
    private Map<String, CachedReply> lastReplyCache;
    private Thread cacheCleaner;

    public class CachedReply {
        long replyTime;
        Map<String, Object> reply;

        public CachedReply(long time, Map<String, Object> reply_) {
            replyTime = time;
            reply = reply_;
        }
    }

    public void putReplyToCache(String corrID, Map<String, Object> reply) {
        if (this.lastReplyCache!=null) {
            CachedReply cachedReply = new CachedReply(System.nanoTime(), reply);
            lastReplyCache.put(corrID, cachedReply);
        }
    }

    public Map<String, Object> getReplyFromCache(String corrID) {
        if (lastReplyCache!=null && lastReplyCache.get(corrID)!=null) return lastReplyCache.get(corrID).reply;
        else {
            if (lastReplyCache!=null) log.debug("No cached reply " + corrID + " on cache !");
            else log.debug("No cache !");
            return null;
        }
    }

    public MsgAkkaAbsRequestActor(MomClient mclient, AppMsgWorker worker, MomMsgTranslator translator_, boolean cache) {
        client = mclient;
        msgWorker = worker;
        translator = translator_;
        isRunning = true;
        if (cache) {
            lastReplyCache = new ConcurrentHashMap<>();
            cacheCleaner = new Thread(new Runnable() {
                @Override
                public void run() {
                    while(isRunning) {
                        try {
                            Thread.sleep(replyCacheRetentionTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        long cleanTime = System.nanoTime();
                        for (String corrID : lastReplyCache.keySet()) {
                            CachedReply cachedReply = lastReplyCache.get(corrID);
                            if ((cleanTime-cachedReply.replyTime)>replyCacheRetentionTime*1000000) lastReplyCache.remove(corrID);
                        }
                    }
                }
            });
            cacheCleaner.start();
        }
    }

    @Override
    public void postStop() {
        if (lastReplyCache!=null) lastReplyCache.clear();
        isRunning = false;
    }

    public MomMsgTranslator getTranslator() {
        return translator;
    }

    public AppMsgWorker getMsgWorker() {
        return msgWorker;
    }

    public MomClient getClient() {
        return client;
    }

    public void setClient(MomClient client) {
        this.client = client;
    }
}