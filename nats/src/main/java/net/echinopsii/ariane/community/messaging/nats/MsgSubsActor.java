/**
 * Messaging - NATS Implementation
 * Message Subscriber Actor
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
import io.nats.client.Message;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsAppMsgWorker;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsSubsActor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class MsgSubsActor extends MsgAkkaAbsSubsActor {
    private static final Logger log = MomLoggerFactory.getLogger(MsgSubsActor.class);

    public static Props props(final AppMsgWorker worker) {
        return Props.create(new Creator<MsgSubsActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgSubsActor create() throws Exception {
                return new MsgSubsActor(worker);
            }
        });
    }

    public MsgSubsActor(AppMsgWorker worker) {
        super(worker, new MsgTranslator());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Message) {
            Map<String, Object> finalMessage = null;
            Map<String, Object> tasteMessage = ((MsgTranslator) super.getTranslator()).decode(new Message[]{(Message) message});

            if (((HashMap)tasteMessage).containsKey(MomMsgTranslator.MSG_SPLIT_COUNT) &&
                    (int)((HashMap)tasteMessage).get(MomMsgTranslator.MSG_SPLIT_COUNT) > 1) {
                if (super.getMsgWorker() instanceof MomAkkaAbsAppMsgWorker) {
                    String msgSplitID = (String) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_MID);
                    Message[] wipMsgChunks;
                    if (!((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsg.containsKey(msgSplitID)) {
                        wipMsgChunks = new Message[(int) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_COUNT)];
                        ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsg.put(msgSplitID, wipMsgChunks);
                        ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsgCount.put(msgSplitID, 0);
                    } else wipMsgChunks = (Message[]) ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsg.get(msgSplitID);

                    wipMsgChunks[(int) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_OID)] = (Message) message;
                    int count = ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsgCount.get(msgSplitID) + 1;
                    ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsgCount.put(msgSplitID, count);

                    if (((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsgCount.get(msgSplitID).equals((int) ((HashMap) tasteMessage).get(MomMsgTranslator.MSG_SPLIT_COUNT))) {
                        finalMessage = ((MsgTranslator) super.getTranslator()).decode(wipMsgChunks);
                        ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsg.remove(msgSplitID);
                        ((MomAkkaAbsAppMsgWorker)super.getMsgWorker()).wipMsgCount.remove(msgSplitID);
                    } else {
                        log.error("High payload splitted messages are not supported by underlying message worker...");
                        log.error(super.getMsgWorker().getClass().getName() + " should extends MomAkkaAbsAppMsgWorker !");
                    }
                }
            } else finalMessage = tasteMessage;

            if (finalMessage!=null) super.getMsgWorker().apply(finalMessage);
        } else
            unhandled(message);
    }
}
