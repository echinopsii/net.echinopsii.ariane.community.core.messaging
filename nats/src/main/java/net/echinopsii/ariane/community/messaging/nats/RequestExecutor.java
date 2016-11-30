/**
 * Messaging - NATS Implementation
 * Request Executor implementation
 * Copyright (C) 4/30/16 echinopsii
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

package net.echinopsii.ariane.community.messaging.nats;

import io.nats.client.Connection;
import io.nats.client.Message;
import io.nats.client.SyncSubscription;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsRequestExecutor;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RequestExecutor extends MomAkkaAbsRequestExecutor implements MomRequestExecutor<String, AppMsgWorker> {
    private static final Logger log = MomLoggerFactory.getLogger(RequestExecutor.class);

    private HashMap<String, HashMap<String, SyncSubscription>> sessionsRPCSubs = new HashMap<>();
    private HashMap<String, Boolean> destinationTrace = new HashMap<>();

    public RequestExecutor(Client client) throws IOException {
        super(client);
    }

    @Override
    public Map<String, Object> fireAndForget(Map<String, Object> request, String destination) {
        String groupID = super.getMomClient().getCurrentMsgGroup();
        if (groupID!=null) destination = groupID + "-" + destination;
        Message message = new MsgTranslator().encode(request);
        message.setSubject(destination);
        try {
            ((Connection)super.getMomClient().getConnection()).publish(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return request;
    }

    @Override
    public Map<String, Object> RPC(Map<String, Object> request, String destination, String replySource, AppMsgWorker answerCB) throws TimeoutException {
        Map<String, Object> response = null;

        String groupID = super.getMomClient().getCurrentMsgGroup();
        if (groupID!=null && !destination.contains(groupID)) {
            destination = groupID + "-" + destination;
            if (replySource==null) replySource = destination + "-RET";
        }

        if (destinationTrace.get(destination)==null) destinationTrace.put(destination, false);
        if (destinationTrace.get(destination)) request.put(MomMsgTranslator.MSG_RETRY_COUNT,true);
        else request.remove(MomMsgTranslator.MSG_RETRY_COUNT);

        Message message = new MsgTranslator().encode(request);
        message.setSubject(destination);
        if (replySource!=null) message.setReplyTo(replySource);

        try {
            Message msgResponse = null;
            long beginWaitingAnswer = 0;
            if (replySource==null) {
                beginWaitingAnswer = System.nanoTime();
                msgResponse = ((Connection) super.getMomClient().getConnection()).request(
                        message.getSubject(), message.getData(), super.getMomClient().getRPCTimout(), TimeUnit.SECONDS
                );
            } else {
                String corrId;
                synchronized (UUID.class) {
                    corrId = UUID.randomUUID().toString();
                }
                request.put(MsgTranslator.MSG_CORRELATION_ID, corrId);

                SyncSubscription subs;
                if (groupID!=null) {
                    if (sessionsRPCSubs.get(groupID) != null) {
                        if (sessionsRPCSubs.get(groupID).get(replySource) != null) subs = sessionsRPCSubs.get(groupID).get(replySource);
                        else {
                            subs = ((Connection)super.getMomClient().getConnection()).subscribeSync(replySource);
                            sessionsRPCSubs.get(groupID).put(replySource, subs);
                        }
                    } else {
                        HashMap<String, SyncSubscription> groupSubs = new HashMap<>();
                        subs = ((Connection)super.getMomClient().getConnection()).subscribeSync(replySource);
                        groupSubs.put(replySource, subs);
                        sessionsRPCSubs.put(groupID, groupSubs);
                    }
                } else subs = ((Connection)super.getMomClient().getConnection()).subscribeSync(replySource);

                ((Connection) super.getMomClient().getConnection()).publish(message);
                long rpcTimeout = super.getMomClient().getRPCTimout() * 1000000000;
                beginWaitingAnswer = System.nanoTime();
                while(msgResponse==null && rpcTimeout >= 0) {
                    try {
                        msgResponse = subs.nextMessage(rpcTimeout, TimeUnit.NANOSECONDS);
                        if (msgResponse!=null) {
                            String responseCorrID = (String) new MsgTranslator().decode(msgResponse).get(MsgTranslator.MSG_CORRELATION_ID);
                            if (responseCorrID != null && !responseCorrID.equals(corrId)) {
                                log.warn("Response discarded ( " + responseCorrID + " ) ...");
                                msgResponse = null;
                            }
                        }
                    } catch (InterruptedException | TimeoutException ex) {
                        log.debug("Thread interrupted while waiting for RPC answer...");
                    } finally {
                        if (super.getMomClient().getRPCTimout()>0)
                            rpcTimeout = super.getMomClient().getRPCTimout()*1000000000 - (System.nanoTime()-beginWaitingAnswer);
                        else rpcTimeout = 0;
                    }
                }
                if (groupID==null) subs.close();
            }

            if (msgResponse!=null) {
                long endWaitingAnswer = System.nanoTime();
                long rpcTime = endWaitingAnswer - beginWaitingAnswer;
                log.debug("RPC time : " + rpcTime);
                if (super.getMomClient().getRPCTimout()>0 && beginWaitingAnswer>0 && rpcTime > super.getMomClient().getRPCTimout()*1000000000*3/5) {
                    destinationTrace.put(destination, true);
                    log.warn("Slow RPC time (" + rpcTime/1000000000 + ") on request to queue " + destination);
                } else  destinationTrace.put(destination, false);
                response = new MsgTranslator().decode(msgResponse);
            } else {
                log.warn("No response returned from request on " + destination + " queue after " +
                        super.getMomClient().getRPCTimout() + " sec...");
                if (request.containsKey(MomMsgTranslator.MSG_RETRY_COUNT)) {
                    int retryCount = (int)request.get(MomMsgTranslator.MSG_RETRY_COUNT);
                    if ((super.getMomClient().getRPCRetry()-retryCount) > 0) {
                        request.put(MomMsgTranslator.MSG_RETRY_COUNT, retryCount+1);
                        destinationTrace.put(destination, true);
                        log.warn("Retry (" + request.get(MomMsgTranslator.MSG_RETRY_COUNT) + ")");
                        return this.RPC(request, destination, replySource, answerCB);
                    } else
                        throw new TimeoutException(
                                "No response returned from request on " + destination + " queue after " +
                                        super.getMomClient().getRPCTimout() + "*" + super.getMomClient().getRPCRetry() + " sec..."
                        );
                } else {
                    request.put(MomMsgTranslator.MSG_RETRY_COUNT, 1);
                    destinationTrace.put(destination, true);
                    log.warn("Retry (" + request.get(MomMsgTranslator.MSG_RETRY_COUNT) + ")");
                    return this.RPC(request, destination, replySource, answerCB);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (answerCB!=null)
            response = answerCB.apply(response);

        return response;
    }

    public void cleanGroupReqResources(String groupID) {
        if (this.sessionsRPCSubs.get(groupID)!=null) {
            for (String replySource : this.sessionsRPCSubs.get(groupID).keySet())
                this.sessionsRPCSubs.get(groupID).get(replySource).close();
            this.sessionsRPCSubs.get(groupID).clear();
            this.sessionsRPCSubs.remove(groupID);
        }
    }

    public void stop() {

    }
}
