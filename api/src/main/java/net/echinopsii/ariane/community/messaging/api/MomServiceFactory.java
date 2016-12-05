/**
 * Ariane Community Messaging
 * Mom Service Factory Interface
 *
 * Copyright (C) 8/24/14 echinopsii
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

package net.echinopsii.ariane.community.messaging.api;

import java.util.List;

/**
 *
 * @param <SRV>
 * @param <W>
 * @param <F>
 * @param <S>
 */
public interface MomServiceFactory<SRV extends MomService, W extends AppMsgWorker, F extends AppMsgFeeder, S> {
    /**
     * Request worker from a source - can manage message grouping (usefull for mono-thread sessions)
     * @param source the source where request are coming from
     * @param requestWorker the application request worker
     * @return service
     */
    SRV msgGroupRequestService(S source, W requestWorker);

    /**
     * Request worker from a source - no transaction possible here.
     * @param source the source where request are coming from
     * @param requestWorker the application request worker
     * @return service
     */
    SRV requestService(S source, W requestWorker);

    /**
     * Feed message to a baseDestination
     * @param baseDestination the baseDestination (must be a topic)
     * @param feeder the application feeder building the message to feed
     * @return service ref
     */
    SRV feederService(S baseDestination, S selector, int interval, F feeder);

    /**
     * Receive message from a feed source
     * @param source the feed source
     * @param feedWorker the feed message worker
     * @return service ref
     */
    SRV subscriberService(S source, S selector, W feedWorker);

    /**
     * Get services list
     * @return the service list
     */
    List<SRV> getServices();
}