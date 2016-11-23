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

public class MomAkkaRequestRouter extends UntypedActor {

    private static final Logger log = MomLoggerFactory.getLogger(MomAkkaRequestRouter.class);
    private boolean willStopSoon = false;

    private Router router = null;
    private Props routeeProps = null;
    private String routeeNamePrefix = null;

    public MomAkkaRequestRouter(Props routeeProps, String routeeNamePrefix, int nbRoutees) {
        this.routeeProps = routeeProps;
        this.routeeNamePrefix = routeeNamePrefix;
        List<Routee> routees = new ArrayList<>();
        for (int i = 0; i < nbRoutees; i++) {
            ActorRef r = getContext().actorOf(routeeProps, routeeNamePrefix + "__" + i);
            getContext().watch(r);
            routees.add(new ActorRefRoutee(r));
        }
        this.router = new Router(new RoundRobinRoutingLogic(), routees);
    }

    public static Props props(final Props props, final String namePrefix, final int nbRoutees) {
        return Props.create(new Creator<MomAkkaRequestRouter>() {
            private static final long serialVersionUID = 1L;

            @Override
            public MomAkkaRequestRouter create() throws Exception {
                return new MomAkkaRequestRouter(props, namePrefix, nbRoutees);
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