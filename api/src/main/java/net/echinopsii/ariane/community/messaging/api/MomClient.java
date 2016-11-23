/**
 * Ariane Community Messaging
 * Mom Client Interface
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

import java.util.Dictionary;
import java.util.Properties;

public interface MomClient {
    String MOM_CLI                        = "mom_cli.impl";
    String MOM_CLI_MSG_DEBUG_ON_TIMEOUT   = "mom_cli.msg_debug_on_timeout";
    String MOM_CLI_ROUTEES_NB_PER_SERVICE = "mom_cli.nb_routees_per_service";

    String MOM_HOST = "mom_host.fqdn";
    String MOM_PORT = "mom_host.port";
    String MOM_USER = "mom_host.user";
    String MOM_PSWD = "mom_host.password";

    // RABBITMQ SPECIFIC PROPERTIES KEYS
    String RBQ_VHOST           = "mom_host.rbq_vhost";
    String RBQ_PRODUCT_KEY     = "mom_cli.rabbitmq.product";
    String RBQ_INFORMATION_KEY = "mom_cli.rabbitmq.information";
    String RBQ_PLATFORM_KEY    = "mom_cli.rabbitmq.platform";
    String RBQ_COPYRIGHT_KEY   = "mom_cli.rabbitmq.copyright";
    String RBQ_VERSION_KEY     = "mom_cli.rabbitmq.version";

    // NATS SPECIFIC PROPERTIES KEYS
    String NATS_CONNECTION_NAME = "mom_cli.nats.connection_name";

    // ARIANE SPECIFIC PROPERTIES KEYS BEGIN WITH
    String ARIANE_KEYS = "ariane";
    String ARIANE_PGURL_KEY = "ariane.pgurl";
    String ARIANE_OSI_KEY = "ariane.osi";
    String ARIANE_OTM_KEY = "ariane.otm";
    String ARIANE_OTM_NOT_DEFINED = "OTM_NOT_DEFINED";
    String ARIANE_CMP_KEY = "ariane.cmp";
    String ARIANE_APP_KEY = "ariane.app";
    String ARIANE_PID_KEY = "ariane.pid";

    String getClientID();

    void   init(Properties properties) throws Exception;
    void   init(Dictionary properties) throws Exception;
    void   close() throws Exception;

    Object getConnection();
    boolean isConnected();
    MomRequestExecutor createRequestExecutor();
    MomServiceFactory getServiceFactory();

    void openMsgGroupRequest(String groupID);
    String getCurrentMsgGroup();
    void closeMsgGroupRequest(String groupID);

    void openMsgGroupService(String groupID);
    void closeMsgGroupService(String groupID);
}