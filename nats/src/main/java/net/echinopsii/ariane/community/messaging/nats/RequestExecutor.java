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
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsRequestExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class RequestExecutor extends MomAkkaAbsRequestExecutor implements MomRequestExecutor<String, AppMsgWorker> {
    private static final Logger log = LoggerFactory.getLogger(RequestExecutor.class);

    private HashMap<String, HashMap<String, SyncSubscription>> sessionsRPCSubs = new HashMap<>();
    private long rpc_timeout = 0;

    public RequestExecutor(Client client) throws IOException {
        super(client);
    }

    public RequestExecutor(Client client, long rpc_timeout) throws IOException {
        super(client);
        this.rpc_timeout = rpc_timeout;
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
    public Map<String, Object> RPC(Map<String, Object> request, String destination, String replySource, AppMsgWorker answerCB) {
        Map<String, Object> response = null;
        Message message = new MsgTranslator().encode(request);

        String groupID = super.getMomClient().getCurrentMsgGroup();
        if (groupID!=null) {
            destination = groupID + "-" + destination;
            if (replySource==null) replySource = destination + "-RET";
        }

        message.setSubject(destination);
        if (replySource!=null) message.setReplyTo(replySource);

        try {
            Message msgResponse = null;
            if (replySource==null) {
                msgResponse = ((Connection) super.getMomClient().getConnection()).request(
                        message.getSubject(), message.getData()
                );
            } else {
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
                long exit_count = 1;
                if (this.rpc_timeout > 0) exit_count = this.rpc_timeout * 100;
                while(msgResponse==null && exit_count > 0) {
                    try {
                        msgResponse = subs.nextMessage(10);
                        if (this.rpc_timeout > 0) exit_count--;
                    } catch (InterruptedException ex) {
                        log.debug("Thread interrupted... Replay");
                    }
                }
                if (groupID==null) subs.close();
            }
            response = new MsgTranslator().decode(msgResponse);
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
