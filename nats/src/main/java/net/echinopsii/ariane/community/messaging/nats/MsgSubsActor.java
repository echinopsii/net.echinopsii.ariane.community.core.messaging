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
import net.echinopsii.ariane.community.messaging.common.MomAkkaAbsAppHPMsgSrvWorker;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsSubsActor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

/**
 * MsgSubsActor class extending {@link net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsSubsActor} abstract class for NATS MoM
 */
public class MsgSubsActor extends MsgAkkaAbsSubsActor {
    private static final Logger log = MomLoggerFactory.getLogger(MsgSubsActor.class);

    /**
     * (internal usage only)
     * Return Akka actor Props to spawn a new MsgRequestActor through Akka.
     * Should not be called outside {@link net.echinopsii.ariane.community.messaging.nats.ServiceFactory#subscriberService(String, String, AppMsgWorker)}
     *
     * @param worker the AppMsgWorker in charge of subscription message feed treatment
     * @return Akka actor Props
     */
    public static Props props(final AppMsgWorker worker) {
        return Props.create(new Creator<MsgSubsActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgSubsActor create() throws Exception {
                return new MsgSubsActor(worker);
            }
        });
    }

    /**
     * (internal usage only)
     * MsgSubsActor constructor. Should not be called outside {@link this#props}
     *
     * @param worker the AppMsgWorker in charge of subscription message feed treatment
     */
    public MsgSubsActor(AppMsgWorker worker) {
        super(worker, new MsgTranslator());
    }

    /**
     * {@link akka.actor.UntypedActor#onReceive(Object)} implementation.
     * if message instance of {@link io.nats.client.Message} :
     * <br/> decode the message
     * <br/> if splitted message cache the current message. if all splitted message has been received rebuild the final message and clear the cache.
     * <br/> else the message is the final message
     * <br/> if final message is not null then request treatment from attached worker.
     * else unhandled
     * @param message the akka message received by actor
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof Message) {
            Map<String, Object> finalMessage = null;
            Map<String, Object> tasteMessage = ((MsgTranslator) super.getTranslator()).decode(new Message[]{(Message) message});

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
                    } else {
                        log.error("High payload splitted messages are not supported by underlying message worker...");
                        log.error(super.getMsgWorker().getClass().getName() + " should extends MomAkkaAbsAppHPMsgSrvWorker !");
                    }
                }
            } else finalMessage = tasteMessage;

            if (finalMessage!=null) super.getMsgWorker().apply(finalMessage);
        } else
            unhandled(message);
    }
}
