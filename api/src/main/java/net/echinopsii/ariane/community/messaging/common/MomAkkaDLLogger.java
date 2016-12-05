/**
 * Messaging - Common Implementation
 * MoM DeadLetter Logger Actor Implementation
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

package net.echinopsii.ariane.community.messaging.common;

import akka.actor.*;
import akka.japi.Creator;
import org.slf4j.Logger;

/**
 * MomAkkaDLLogger : an actor to log dead letter
 */
public class MomAkkaDLLogger extends UntypedActor {

    private static final Logger log = MomLoggerFactory.getLogger(MomAkkaDLLogger.class);

    /**
     * @return Akka Props to create an actor for MomAkkaDLLogger
     */
    public static Props props() {
        return Props.create(new Creator<MomAkkaDLLogger>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MomAkkaDLLogger create() throws Exception {
                return new MomAkkaDLLogger();
            }
        });
    }

    /**
     * Message treatment.
     * if input message is instanceof DeadLetter then log warn the dead letter
     * else pass input message to unhandled
     * @see akka.actor.UntypedActor#unhandled(Object)
     * @param message to treat.
     */
    @Override
    public void onReceive(Object message) {
        if (message instanceof DeadLetter) {
            final DeadLetter d = (DeadLetter) message;
            log.warn("DeadLetter " + d.message().toString() + " received. Recipient was " + d.recipient().path().name() + " .");
        } else {
            unhandled(message);
        }
    }
}