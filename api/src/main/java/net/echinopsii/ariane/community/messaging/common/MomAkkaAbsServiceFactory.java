/**
 * Messaging - Common Implementation
 * Abstract Service implementation
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

package net.echinopsii.ariane.community.messaging.common;

import net.echinopsii.ariane.community.messaging.api.*;

import java.util.ArrayList;
import java.util.List;

public abstract class MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {

    private MomClient momClient ;
    private List<MomAkkaService> serviceList  = new ArrayList<MomAkkaService>();

    public MomAkkaAbsServiceFactory(MomClient client) {
        momClient = client;
    }

    @Override
    public List<MomAkkaService> getServices() {
        return serviceList;
    }

    public MomClient getMomClient() { return momClient;}
}