/**
 * Messaging - Common Implementation
 * Abstract Service implementation
 * Copyright (C) $/30/16 echinopsii
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

/**
 * MomAkkaAbsServiceFactory provides an abstract implementation of MomServiceFactory interface based on actors model and Akka.
 */
public abstract class MomAkkaAbsServiceFactory implements MomServiceFactory<MomAkkaService, AppMsgWorker, AppMsgFeeder, String> {

    private MomAkkaAbsClient momClient ;
    private List<MomAkkaService> serviceList  = new ArrayList<MomAkkaService>();

    /**
     * Constructor
     * @param client : the MomClient this request executor will work with
     */
    public MomAkkaAbsServiceFactory(MomAkkaAbsClient client) {
        momClient = client;
    }

    /**
     * @return services created through this service factory.
     */
    @Override
    public List<MomAkkaService> getServices() {
        return serviceList;
    }

    /**
     * @return he MomAkkaAbsClient defined with this service factory.
     */
    public MomAkkaAbsClient getMomClient() { return momClient;}
}