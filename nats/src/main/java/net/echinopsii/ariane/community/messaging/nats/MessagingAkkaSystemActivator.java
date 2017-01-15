/**
 * Injector messaging
 * Injector akka system activator
 * Copyright (C) 2014 Mathilde Ffrench
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

import akka.actor.ActorSystem;
import akka.osgi.ActorSystemActivator;
import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MessagingAkkaSystemActivator to configure and activate Akka system on OSGI environments
 */
public class MessagingAkkaSystemActivator extends ActorSystemActivator {
    private static final Logger log = MomLoggerFactory.getLogger(MessagingAkkaSystemActivator.class);

    private static ActorSystem system ;

    /**
     * @return activated actor system
     */
    public static ActorSystem getSystem() {
        return system;
    }

    /**
     * @param context the OSGI bundle context
     * @param system the ActorSystem to use in this bundle context
     */
    @Override
    public void configure(BundleContext context, ActorSystem system) {
        this.system = system;
        log.info("Ariane Messaging NATS Akka System activated");
    }
}