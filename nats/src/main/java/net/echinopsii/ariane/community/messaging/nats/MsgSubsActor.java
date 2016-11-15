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
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import net.echinopsii.ariane.community.messaging.common.MsgAkkaAbsSubsActor;
import org.slf4j.Logger;

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
            Map<String, Object> finalMessage = ((MsgTranslator) super.getTranslator()).decode((Message) message);
            super.getMsgWorker().apply(finalMessage);
        } else
            unhandled(message);
    }
}
