/**
 * Messaging - Common Implementation
 * MoM Supervisor Actor Abstract Implementation
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
import akka.pattern.Patterns;
import akka.util.Timeout;
import org.slf4j.Logger;
import scala.concurrent.Await;
import scala.concurrent.Future;
import scala.concurrent.duration.Duration;

/**
 * MomAkkaSupervisor : an actor to handle services supervision
 */
public class MomAkkaSupervisor extends UntypedActor {

    private static final Logger log = MomLoggerFactory.getLogger(MomAkkaSupervisor.class);
    private boolean willStopSoon = false;

    private static class MomAkkaSupervisorNewActorReq {
        Props props;
        String name;

        public MomAkkaSupervisorNewActorReq(Props props, String name) {
            this.props = props;
            this.name = name;
        }
    }

    /**
     * Create a new supervised service as a child of an actor
     * @param supervisorAR the supervisor parent actor ref in charge of creating the new service
     * @param props Akka Props to create the new supervised service actor
     * @param name the supervised service name
     * @return the new supervised service actor ref
     */
    public static ActorRef createNewSupervisedService(ActorRef supervisorAR, Props props, String name) {
        Timeout timeout = new Timeout(Duration.create(2, "seconds"));
        Future<Object> futureAR = Patterns.ask(supervisorAR, new MomAkkaSupervisorNewActorReq(props, name), timeout);
        ActorRef ret = null;
        try {
            ret = (ActorRef) Await.result(futureAR, timeout.duration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * send a stop soon notification to actor identified by ar
     * @param ar actorRef which identify the actor to notify
     */
    public static void willStopSoon(ActorRef ar) {
        ar.tell("stop_soon", null);
    }

    /**
     * @return Akka Props to create an actor for MomAkkaSupervisor
     */
    public static Props props() {
        return Props.create(new Creator<MomAkkaSupervisor>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MomAkkaSupervisor create() throws Exception {
                return new MomAkkaSupervisor();
            }
        });
    }

    private ActorRef actorOf(Props props, String name) {
        return getContext().actorOf(props, name);
    }

    /**
     * Message treatment.
     * if message is "kill" then unwatch and stop cleanly all supervised child actors and finally send a poison pill to any child still alive
     * else if message is "stop_soon" set boolean accordingly
     * else if message instanceof Terminated log warn which actor has been terminated
     * else if message instanceof MomAkkaSupervisorNewActorReq create a new supervised service
     * else unhandled this message
     * @param message to treat.
     */
    @Override
    public void onReceive(Object message) {
        if (message.equals("kill")) {
            for (ActorRef each : getContext().getChildren()) {
                getContext().unwatch(each);
                getContext().stop(each);
            }
            for (ActorRef each : getContext().getChildren()) if (!each.isTerminated()) each.tell(PoisonPill.getInstance(), null);
        } else if (message.equals("stop_soon")) {
            this.willStopSoon = true;
        } else if (message instanceof Terminated) {
            final Terminated t = (Terminated) message;
            if (willStopSoon) log.debug("Actor " + t.getActor().path().name() + " is terminated.");
            else log.warn("Actor " + t.getActor().path().name() + " is terminated.");
        } else if (message instanceof MomAkkaSupervisorNewActorReq) {
            final MomAkkaSupervisorNewActorReq req = (MomAkkaSupervisorNewActorReq) message;

            try {
                ActorRef ref = actorOf(req.props, req.name);
                getSender().tell(ref, getSelf());
            } catch (Exception e) {
                getSender().tell(new akka.actor.Status.Failure(e), getSelf());
                throw e;
            }
        } else {
            unhandled(message);
        }
    }
}