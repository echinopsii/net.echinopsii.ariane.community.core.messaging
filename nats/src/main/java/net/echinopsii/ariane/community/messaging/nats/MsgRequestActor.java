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
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsRequestActor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;

public class MsgRequestActor extends MsgAkkaAbsRequestActor {
    private static final Logger log = MomLoggerFactory.getLogger(MsgRequestActor.class);

    public static Props props(final Client mclient, final AppMsgWorker worker) {
        return Props.create(new Creator<MsgRequestActor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MsgRequestActor create() throws Exception {
                return new MsgRequestActor(mclient, worker);
            }
        });
    }

    public MsgRequestActor(Client mclient, AppMsgWorker worker) {
        super(mclient, worker, new MsgTranslator());
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (message instanceof Message) {
            try {
                Map<String, Object> finalMessage = ((MsgTranslator)super.getTranslator()).decode((Message) message);
                if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) {
                    if (super.getClient().isMsgDebugOnTimeout()) ((MomLogger)log).setTraceLevel(true);
                    else finalMessage.remove(MomMsgTranslator.MSG_TRACE);
                }
                ((MomLogger)log).traceMessage("MsgRequestActor.onReceive - in", finalMessage);
                Map<String, Object> reply = super.getMsgWorker().apply(finalMessage);
                if (((Message)message).getReplyTo() != null && reply!=null) {
                    if (finalMessage.get(MsgTranslator.MSG_CORRELATION_ID)!=null) reply.put(
                            MsgTranslator.MSG_CORRELATION_ID, finalMessage.get(MsgTranslator.MSG_CORRELATION_ID)
                    );
                    if (super.getClient().getClientID()!=null)
                        reply.put(MsgTranslator.MSG_APPLICATION_ID, super.getClient().getClientID());
                    Message replyMessage = ((MsgTranslator) super.getTranslator()).encode(reply);
                    replyMessage.setSubject(((Message)message).getReplyTo());
                    ((Connection)super.getClient().getConnection()).publish(replyMessage);
                }
                ((MomLogger)log).traceMessage("MsgRequestActor.onReceive - out", finalMessage);
                if (((HashMap)finalMessage).containsKey(MomMsgTranslator.MSG_TRACE)) ((MomLogger)log).setTraceLevel(false);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else
            unhandled(message);
    }
}
