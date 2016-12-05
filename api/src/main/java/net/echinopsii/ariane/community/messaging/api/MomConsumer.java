/**
 * Ariane Community Messaging
 * Mom Consumer Interface
 *
 * Copyright (C) 8/25/14 echinopsii
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

/**
 * MomConsumer interface is a runnable with few helpers method
 * Used internally to implement message consumers which then dispatch
 * immediately to AppMsgWoker actors.
 */
public interface MomConsumer extends Runnable {
    /**
     *
     * @return true if running else false
     */
    boolean isRunning();

    /**
     * start consuming messaging resource
     */
    void    start();

    /**
     * stop consuming messaging resource
     */
    void    stop();
}