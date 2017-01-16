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

/**
 * RequestExecutor class extending {@link net.echinopsii.ariane.community.messaging.common.MomAkkaAbsRequestExecutor} abstract class
 * and implements {@link net.echinopsii.ariane.community.messaging.api.MomRequestExecutor} for NATS MoM.
 */
public class RequestExecutor extends MomAkkaAbsRequestExecutor implements MomRequestExecutor<String, AppMsgWorker> {
    private static final Logger log = MomLoggerFactory.getLogger(RequestExecutor.class);

    private HashMap<String, HashMap<String, SyncSubscription>> sessionsRPCSubs = new HashMap<>();
    private HashMap<String, Boolean> destinationTrace = new HashMap<>();

    /**
     * @param client an initialized NATS Client
     */
    public RequestExecutor(Client client) {
        super(client);
    }

    private void initMsgSplitGroup(String destination, String msgSplitMID, String msgSplitDest) throws TimeoutException {
        Map<String, Object> request = new HashMap<>();
        request.put(MomMsgTranslator.OPERATION_FDN, MomMsgTranslator.OP_MSG_SPLIT_FEED_INIT);
        request.put(MomMsgTranslator.PARAM_MSG_SPLIT_MID, msgSplitMID);
        request.put(MomMsgTranslator.PARAM_MSG_SPLIT_FEED_DEST, msgSplitDest);
        this.RPC(request, destination, msgSplitDest + "_INIT_RET", null);
    }

    private void endMsgSplitGroup(String destination, String msgSplitMID, String msgSplitDest) throws TimeoutException {
        Map<String, Object> request = new HashMap<>();
        request.put(MomMsgTranslator.OPERATION_FDN, MomMsgTranslator.OP_MSG_SPLIT_FEED_INIT);
        request.put(MomMsgTranslator.PARAM_MSG_SPLIT_MID, msgSplitMID);
        this.RPC(request, destination, msgSplitDest + "_END_RET", null);
    }

    /**
     * Fire And Forget : send request to target destination and manage message split if needed
     * @param request the request message
     * @param destination the target destination queue
     * @return the sent request
     */
    @Override
    public Map<String, Object> FAF(Map<String, Object> request, String destination) {
        request.put(MsgTranslator.MSG_NATS_SUBJECT, destination);
        Message[] messages = new MsgTranslator().encode(request);

        String groupID = super.getMomClient().getCurrentMsgGroup();
        if (groupID!=null) destination = groupID + "-" + destination;

        String splitMID = null;
        if (groupID==null && messages.length>1) {
            Map<String, Object> tasteMsg = new MsgTranslator().decode(new Message[]{messages[0]});
            splitMID = (String) tasteMsg.get(MomMsgTranslator.MSG_SPLIT_MID);
            try {
                initMsgSplitGroup(destination, splitMID, destination + "_" + splitMID);
                destination += "_" + splitMID;
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        for (Message message : messages) {
            try {
                message.setSubject(destination);
                ((Connection) super.getMomClient().getConnection()).publish(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (groupID==null && messages.length>1) {
            destination = destination.split("_" + splitMID)[0];
            try {
                endMsgSplitGroup(destination, splitMID, destination + "_" + splitMID);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        return request;
    }

    /**
     * Remote procedure call : send request to target destination (manage message split if needed) and wait answer to be treated
     * by the answer worker.
     * @param request the request message
     * @param destination the target destination queue
     * @param answerSource the source to get the answer from
     * @param answerWorker the worker object to treat the answer (can be null)
     * @return the answer (treated or not by answer worker)
     * @throws TimeoutException if no answers has been receiver after timeout * retry as configured in NATS Client provided to this RequestExecutor
     */
    @Override
    public Map<String, Object> RPC(Map<String, Object> request, String destination, String answerSource, AppMsgWorker answerWorker) throws TimeoutException {
        Map<String, Object> response = null;

        String groupID = super.getMomClient().getCurrentMsgGroup();
        if (groupID!=null && !destination.contains(groupID)) {
            destination = groupID + "-" + destination;
            if (answerSource == null) answerSource = destination + "-RET";
        }

        String corrId;
        if (request.get(MsgTranslator.MSG_CORRELATION_ID)==null) {
            synchronized (UUID.class) {
                corrId = UUID.randomUUID().toString();
            }
            request.put(MsgTranslator.MSG_CORRELATION_ID, corrId);
        } else corrId = (String) request.get(MsgTranslator.MSG_CORRELATION_ID);

        request.put(MsgTranslator.MSG_NATS_SUBJECT, destination);
        Message[] messages = new MsgTranslator().encode(request);
        String splitMID = null;
        if (groupID==null && messages.length>1) {
            Map<String, Object> tasteMsg = new MsgTranslator().decode(new Message[]{messages[0]});
            splitMID = (String) tasteMsg.get(MomMsgTranslator.MSG_SPLIT_MID);
            try {
                initMsgSplitGroup(destination, splitMID, destination + "_" + splitMID);
                destination += "_" + splitMID;
                if (answerSource == null) answerSource = destination + "-RET";
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        if (destinationTrace.get(destination)==null) destinationTrace.put(destination, false);
        if (destinationTrace.get(destination)) request.put(MomMsgTranslator.MSG_TRACE, true);
        else request.remove(MomMsgTranslator.MSG_TRACE);

        if (answerSource==null)
            synchronized (UUID.class) {
                answerSource = destination + "-" + UUID.randomUUID().toString() + "-RET";
            }

        for (Message message : messages) {
            message.setSubject(destination);
            message.setReplyTo(answerSource);
        }

        try {
            Message[] msgResponse = null;
            Message wipMsgResponse = null;
            long beginWaitingAnswer;

            SyncSubscription subs;
            if (groupID!=null) {
                if (sessionsRPCSubs.get(groupID) != null) {
                    if (sessionsRPCSubs.get(groupID).get(answerSource) != null) subs = sessionsRPCSubs.get(groupID).get(answerSource);
                    else {
                        subs = ((Connection)super.getMomClient().getConnection()).subscribeSync(answerSource);
                        sessionsRPCSubs.get(groupID).put(answerSource, subs);
                    }
                } else {
                    HashMap<String, SyncSubscription> groupSubs = new HashMap<>();
                    subs = ((Connection)super.getMomClient().getConnection()).subscribeSync(answerSource);
                    groupSubs.put(answerSource, subs);
                    sessionsRPCSubs.put(groupID, groupSubs);
                }
            } else subs = ((Connection)super.getMomClient().getConnection()).subscribeSync(answerSource);

            if (destinationTrace.get(destination)) log.info("send request " + corrId);
            for (Message message:messages)
                ((Connection) super.getMomClient().getConnection()).publish(message);

            long rpcTimeout = super.getMomClient().getRPCTimout() * 1000000000;
            beginWaitingAnswer = System.nanoTime();
            int responseSplitCount = 1;
            int responseMsgCount = 0;
            while(wipMsgResponse==null && responseMsgCount < responseSplitCount && rpcTimeout >= 0) {
                try {
                    wipMsgResponse = subs.nextMessage(rpcTimeout, TimeUnit.NANOSECONDS);
                    if (wipMsgResponse!=null) {
                        Map<String, Object> tmpDecodedMsg = new MsgTranslator().decode(new Message[]{wipMsgResponse});
                        String responseCorrID = (String) tmpDecodedMsg.get(MsgTranslator.MSG_CORRELATION_ID);
                        if (responseCorrID != null && !responseCorrID.equals(corrId)) {
                            log.warn("Response discarded ( " + responseCorrID + " ) ...");
                            wipMsgResponse = null;
                        } else {
                            if (msgResponse==null) {
                                responseSplitCount = (int) ((tmpDecodedMsg.get(MsgTranslator.MSG_SPLIT_COUNT)!=null) ? tmpDecodedMsg.get(MsgTranslator.MSG_SPLIT_COUNT) : 1);
                                msgResponse = new Message[responseSplitCount];
                            }
                            int splitOID = (int) ((tmpDecodedMsg.get(MsgTranslator.MSG_SPLIT_OID)!=null) ? tmpDecodedMsg.get(MsgTranslator.MSG_SPLIT_OID) : 0);
                            msgResponse[splitOID] = wipMsgResponse;
                            wipMsgResponse = null;
                            responseMsgCount++;
                        }
                    }
                } catch (InterruptedException | TimeoutException ex) {
                    log.debug("Thread interrupted while waiting for RPC answer...");
                } finally {
                    if (super.getMomClient().getRPCTimout()>0)
                        rpcTimeout = super.getMomClient().getRPCTimout()*1000000000 - (System.nanoTime()-beginWaitingAnswer);
                    else rpcTimeout = 0;
                    if (destinationTrace.get(destination)) log.info("rpcTimeout left: " + rpcTimeout);
                }
            }
            if (groupID==null) subs.close();

            if (msgResponse!=null) {
                long endWaitingAnswer = System.nanoTime();
                long rpcTime = endWaitingAnswer - beginWaitingAnswer;
                log.debug("RPC time : " + rpcTime);
                if (super.getMomClient().getRPCTimout()>0 && beginWaitingAnswer>0 && rpcTime > super.getMomClient().getRPCTimout()*1000000000*3/5) {
                    log.debug("Slow RPC time (" + rpcTime/1000000000 + ") on request to queue " + destination);
                } else destinationTrace.put(destination, false);
                response = new MsgTranslator().decode(msgResponse);
            } else {
                if (request.containsKey(MomMsgTranslator.MSG_RETRY_COUNT)) {
                    int retryCount = (int)request.get(MomMsgTranslator.MSG_RETRY_COUNT);
                    log.warn("No response returned from request on " + destination + " queue after (" +
                            super.getMomClient().getRPCTimout() + "*" + retryCount + 1 + ") sec...");
                    if ((super.getMomClient().getRPCRetry()-retryCount+1) > 0) {
                        request.put(MomMsgTranslator.MSG_RETRY_COUNT, retryCount+1);
                        destinationTrace.put(destination, true);
                        log.warn("Retry (" + request.get(MomMsgTranslator.MSG_RETRY_COUNT) + ")");
                        return this.RPC(request, destination, answerSource, answerWorker);
                    } else
                        throw new TimeoutException(
                                "No response returned from request on " + destination + " queue after " +
                                        super.getMomClient().getRPCTimout() + "*" + super.getMomClient().getRPCRetry() + " sec..."
                        );
                } else {
                    request.put(MomMsgTranslator.MSG_RETRY_COUNT, 1);
                    return this.RPC(request, destination, answerSource, answerWorker);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (groupID==null && messages.length>1) {
            destination = destination.split("_" + splitMID)[0];
            try {
                endMsgSplitGroup(destination, splitMID, destination + "_" + splitMID);
            } catch (TimeoutException e) {
                e.printStackTrace();
            }
        }

        if (answerWorker!=null)
            response = answerWorker.apply(response);

        return response;
    }

    /**
     * close groupID message group answer subscriptions and clean registry
     * @param groupID message group ID
     */
    public void cleanGroupReqResources(String groupID) {
        if (this.sessionsRPCSubs.get(groupID)!=null) {
            for (String replySource : this.sessionsRPCSubs.get(groupID).keySet())
                this.sessionsRPCSubs.get(groupID).get(replySource).close();
            this.sessionsRPCSubs.get(groupID).clear();
            this.sessionsRPCSubs.remove(groupID);
        }
    }

    /**
     * clear and close this resources
     */
    public void stop() {
    }
}
