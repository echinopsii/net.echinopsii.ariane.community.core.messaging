/**
 * Messaging - NATS Implementation
 * ServiceFactory implementation
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

import net.echinopsii.ariane.community.messaging.api.AppMsgFeeder;
import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;

import java.util.List;

public class ServiceFactory implements MomServiceFactory<Service, AppMsgWorker, AppMsgFeeder, String> {

    @Override
    public Service requestService(String source, AppMsgWorker requestCB) {
        return null;
    }

    @Override
    public Service feederService(String baseDestination, String selector, int interval, AppMsgFeeder feederCB) {
        return null;
    }

    @Override
    public Service subscriberService(String source, String selector, AppMsgWorker feedCB) {
        return null;
    }

    @Override
    public List<Service> getServices() {
        return null;
    }
}
