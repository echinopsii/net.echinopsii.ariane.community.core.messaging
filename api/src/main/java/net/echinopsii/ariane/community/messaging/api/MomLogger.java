/**
 * Ariane Community Messaging
 * Mom Logger Interface
 *
 * Copyright (C) 11/13/16 echinopsii
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

import org.slf4j.Logger;
import org.slf4j.spi.LocationAwareLogger;

import java.io.Serializable;
import java.util.Map;

/**
 * MomLogger interface extends slf4j logger interface to help log and debug message flow treatment on
 * the MomClient services.
 * <p/>
 * When msg trace level is enabled any log following the message treatment will be printed (whatever the original
 * log level configured by class) according to the message treatment thread ID.
 *
 */
public interface MomLogger extends Serializable, Logger, LocationAwareLogger {
    /**
     * Define if the message trace will be available through the message treatment.
     * @param isTraceLevelEnabled
     * @return this
     */
    MomLogger setMsgTraceLevel(boolean isTraceLevelEnabled);

    /**
     * Trace a message
     * @param opsName the operation name (basically the class and method where this method is called)
     * @param message the message to trace
     * @param ignoredFields the list of message fields to ignore in the trace
     */
    void traceMessage(String opsName, Map<String, Object> message, String... ignoredFields);
}
