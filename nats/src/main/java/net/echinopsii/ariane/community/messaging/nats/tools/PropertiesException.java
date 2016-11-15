/**
 * Properties Serialization Exception
 *
 * Copyright (C) 2014  Mathilde Ffrench
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

package net.echinopsii.ariane.community.messaging.nats.tools;

import net.echinopsii.ariane.community.messaging.common.MomLoggerFactory;
import org.slf4j.Logger;

public class PropertiesException extends Exception {
    private static final Logger log = MomLoggerFactory.getLogger(PropertiesException.class);
    private static final long serialVersionUID = -5787356297845468334L;

    public PropertiesException() { super(); }

    public PropertiesException(String message) { super(message); }
}