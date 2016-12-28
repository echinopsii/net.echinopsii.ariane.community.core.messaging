/**
 * Messaging - NATS Implementation
 * Message Request Actor
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

import akka.actor.Props;
import akka.japi.Creator;
import io.nats.client.Connection;
import io.nats.client.Message;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomLogger;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsAppHPMsgSrvWorker;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsRequestActor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class MsgRequestActor extends MsgAkkaAbsRequestActor {
    private static final Logger log = MomLoggerFactory.getLogger(MsgRequestActor.class);
    //private static HashMap<String, Integer> wipMsgCount = new HashMap<>();
    //private static HashMap<String, Message[]> wipMsg = new HashMap<>();

    public static Props props(final Client mclient, final AppMsgWorker worker, final boolean cache) {
        return Props.create(new Creator<MsgRequestActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgRequestActor create() throws Exception {
                return new MsgRequestActor(mclient, worker, cache);
            }
        });
    }

    public MsgRequestActor(Client mclient, AppMsgWorker worker, boolean cache) {
        super(mclient, worker, new MsgTranslator(), cache);
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Message) {
            try {
                Map<String, Object> finalMessage = null;
                Map<String, Object> tasteMessage = ((MsgTranslator)super.getTranslator()).decode(new Message[]{(Message) message});
                if (((HashMap)tasteMessage).containsKey(MomMsgTranslator.MSG_TRACE)) {
                    if (super.getClient().isMsgDebugOnTimeout()) ((MomLogger)log).setMsgTraceLevel(true);
                    else tasteMessage.remove(MomMsgTranslator.MSG_TRACE);
                }

                boolean errorOnSplit = false;
                if (((HashMap)tasteMessage).containsKey(MomMsgTranslator.MSG_SPLIT_COUNT) &&
                        (int)((HashMap)tasteMessage).get(MomMsgTranslator.MSG_SPLIT_COUNT) > 1) {
                    if (super.getMsgWorker() instanceof MomAkkaAbsAppHPMsgSrvWorker) {
                        String msgSplitID = (String) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_MID);
                        Message[] wipMsgChunks;
                        if (!((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsg.containsKey(msgSplitID)) {
                            wipMsgChunks = new Message[(int) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_COUNT)];
                            ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsg.put(msgSplitID, wipMsgChunks);
                            ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsgCount.put(msgSplitID, 0);
                        } else wipMsgChunks = (Message[]) ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsg.get(msgSplitID);

                        wipMsgChunks[(int) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_OID)] = (Message) message;
                        int count = ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsgCount.get(msgSplitID) + 1;
                        ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsgCount.put(msgSplitID, count);

                        if (((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsgCount.get(msgSplitID).equals((int) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_COUNT))) {
                            finalMessage = ((MsgTranslator) super.getTranslator()).decode(wipMsgChunks);
                            ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsg.remove(msgSplitID);
                            ((MomAkkaAbsAppHPMsgSrvWorker)super.getMsgWorker()).wipMsgCount.remove(msgSplitID);
                        }
                    } else {
                        log.error("High payload splitted messages are not supported by underlying message worker...");
                        log.error(super.getMsgWorker().getClass().getName() + " should extends MomAkkaAbsAppHPMsgSrvWorker !");
                        finalMessage = tasteMessage;
                        errorOnSplit = true;
                    }
                } else finalMessage = tasteMessage;

                if (finalMessage!=null) {
                    ((MomLogger) log).traceMessage("MsgRequestActor.onReceive - in", finalMessage);
                    if (!errorOnSplit) {
                        Map<String, Object> reply = null;
                        if (finalMessage.get(MsgTranslator.MSG_CORRELATION_ID) != null &&
                                super.getReplyFromCache((String) finalMessage.get(MsgTranslator.MSG_CORRELATION_ID)) != null)
                            reply = super.getReplyFromCache((String) finalMessage.get(MsgTranslator.MSG_CORRELATION_ID));
                        if (reply == null) reply = super.getMsgWorker().apply(finalMessage);
                        else log.debug("reply from cache !");

                        if (finalMessage.get(MsgTranslator.MSG_CORRELATION_ID) != null)
                            super.putReplyToCache((String) finalMessage.get(MsgTranslator.MSG_CORRELATION_ID), reply);

                        if (((Message) message).getReplyTo() != null && reply != null) {
                            if (finalMessage.get(MsgTranslator.MSG_CORRELATION_ID) != null) reply.put(
                                    MsgTranslator.MSG_CORRELATION_ID, finalMessage.get(MsgTranslator.MSG_CORRELATION_ID)
                            );
                            if (super.getClient().getClientID() != null)
                                reply.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
                            Message[] replyMessage = ((MsgTranslator) super.getTranslator()).encode(reply);
                            for (Message msg : replyMessage) {
                                msg.setSubject(((Message) message).getReplyTo());
                                ((Connection) super.getClient().getConnection()).publish(msg);
                            }
                        }

                        ((MomLogger) log).traceMessage("MsgRequestActor.onReceive - out", finalMessage);
                        if (((HashMap) finalMessage).containsKey(MomMsgTranslator.MSG_TRACE))
                            ((MomLogger) log).setMsgTraceLevel(false);
                    } else if (((Message) message).getReplyTo() != null) {
                        Map<String, Object> reply = new HashMap<>();
                        reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_SERVER_ERR);
                        reply.put(MomMsgTranslator.MSG_ERR, "High payload splitted messages are not supported by underlying message worker");
                        Message[] replyMessage = ((MsgTranslator) super.getTranslator()).encode(reply);
                        for (Message msg : replyMessage) {
                            msg.setSubject(((Message) message).getReplyTo());
                            ((Connection) super.getClient().getConnection()).publish(msg);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else
            unhandled(message);
    }
}
