/**
 * Messaging - NATS Implementation
 * Client implementation
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
package net.echinopsii.ariane.community.messaging.nats;

import net.echinopsii.ariane.community.messaging.api.MomClient;
import net.echinopsii.ariane.community.messaging.api.MomRequestExecutor;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Dictionary;
import java.util.Properties;

public class Client implements MomClient {

    private static final Logger log = LoggerFactory.getLogger(Client.class);

    @Override
    public String getClientID() {
        return null;
    }

    @Override
    public void init(Properties properties) throws Exception {

    }

    @Override
    public void init(Dictionary properties) throws Exception {

    }

    @Override
    public void close() throws Exception {

    }

    @Override
    public Object getConnection() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public MomRequestExecutor createRequestExecutor() {
        return null;
    }

    @Override
    public MomServiceFactory getServiceFactory() {
        return null;
    }
}
