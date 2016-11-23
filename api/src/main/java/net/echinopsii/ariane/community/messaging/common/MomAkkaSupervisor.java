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

public class MomAkkaSupervisor extends UntypedActor {

    private static final Logger log = MomLoggerFactory.getLogger(MomAkkaSupervisor.class);
    private boolean willStopSoon = false;

    public static class MomAkkaNewActorReq {
        Props props;
        String name;

        public MomAkkaNewActorReq(Props props, String name) {
            this.props = props;
            this.name = name;
        }
    }

    public static ActorRef createNewSupervisedService(ActorRef ar, Props props, String name) {
        Timeout timeout = new Timeout(Duration.create(2, "seconds"));
        Future<Object> futureAR = Patterns.ask(ar, new MomAkkaNewActorReq(props, name), timeout);
        ActorRef ret = null;
        try {
            ret = (ActorRef) Await.result(futureAR, timeout.duration());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ret;
    }

    public static void willStopSoon(ActorRef ar) {
        ar.tell("stop_soon", null);
    }

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

    @Override
    public void onReceive(Object message) throws Exception {
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
        } else if (message instanceof MomAkkaNewActorReq) {
            final MomAkkaNewActorReq req = (MomAkkaNewActorReq) message;

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