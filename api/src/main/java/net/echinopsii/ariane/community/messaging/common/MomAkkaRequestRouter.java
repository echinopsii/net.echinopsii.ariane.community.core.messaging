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
import akka.routing.ActorRefRoutee;
import akka.routing.RoundRobinRoutingLogic;
import akka.routing.Routee;
import akka.routing.Router;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * MomAkkaRequestRouter : an actor to dispatch requests to worker routees (round robin)
 */
public class MomAkkaRequestRouter extends UntypedActor {

    private static final Logger log = MomLoggerFactory.getLogger(MomAkkaRequestRouter.class);
    private boolean willStopSoon = false;

    private Router router = null;
    private Props routeeProps = null;
    private String routeeNamePrefix = null;

    /**
     * Constructor
     * @param routeeProps routee Akka props to create worker actors
     * @param routeeNamePrefix routee name prefix
     * @param routeesCount routees count to be managed
     */
    public MomAkkaRequestRouter(Props routeeProps, String routeeNamePrefix, int routeesCount) {
        this.routeeProps = routeeProps;
        this.routeeNamePrefix = routeeNamePrefix;
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < routeesCount; i++) {
            ActorRef r = getContext().actorOf(routeeProps, routeeNamePrefix + "__" + i);
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        this.router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    /**
     * @param routeeProps routee Akka props to create worker actors
     * @param routeeNamePrefix routee name prefix
     * @param routeesCount routees count to be managed
     * @return Akka Props to create an actor for MomAkkaRequestRouter
     */
    public static Props props(final Props routeeProps, final String routeeNamePrefix, final int routeesCount) {
        return Props.create(new Creator<MomAkkaRequestRouter>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MomAkkaRequestRouter create() throws Exception {
                return new MomAkkaRequestRouter(routeeProps, routeeNamePrefix, routeesCount);
            }
        });
    }

    private ActorRef actorOf(Props props, String name) {
        return getContext().actorOf(props, name);
    }

    /**
     * Message treatment.
     * if input message is "kill"
     * else if message instanceof Terminated create a new routee if !willStopSoon
     * else route message to routees (round robing)
     * @param message
     */
    @Override
    public void onReceive(Object message) {
        if (message.equals("kill")) {
            for (ActorRef each : getContext().getChildren()) {
                getContext().unwatch(each);
                getContext().stop(each);
            }
            for (ActorRef each : getContext().getChildren()) if (!each.isTerminated()) each.tell(PoisonPill.getInstance(), null);
        } else if (message instanceof Terminated) {
            final Terminated t = (Terminated) message;
            if (willStopSoon) log.debug("Routee " + t.getActor().path().name() + " is terminated.");
            else {
                log.info("Routee " + t.getActor().path().name() + " is terminated. Create new routee.");
                String routeeIdx = t.actor().path().name().split("__")[1];
                this.router = this.router.removeRoutee(t.actor());
                ActorRef r = getContext().actorOf(this.routeeProps, this.routeeNamePrefix + "__" + routeeIdx);
                getContext().watch(r);
                this.router = this.router.addRoutee(new ActorRefRoutee(r));
            }
        } else {
            router.route(message, getSender());
        }
    }
}