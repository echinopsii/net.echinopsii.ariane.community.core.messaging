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

/**
 * MomClient interface.
 * A class implementing this interface will manage the MoM broker connection.
 *
 * This is the entry point :
 * - to create new services via this MomClient MomServiceFactory.
 * - to create on demand message group service based on the existing service.
 * - to create new requests executors.
 * - to create new message group requests.
 *
 * The resources created through MomClient are intended to be stored on registries
 * to be properly managed while closing MomClient.
 *
 * Finally this is also the entry point to manage tuning configuration like :
 * - define the on message debug option on services
 * - define the RPC timeout (sec)
 * - define the RPC retry number
 *
 * Abstract implementation :
 * @see net.echinopsii.ariane.community.messaging.common.MomAkkaAbsClient
 *
 */
public interface MomClient {
    String MOM_CLI                        = "mom_cli.impl";
    String MOM_CLI_MSG_DEBUG_ON_TIMEOUT   = "mom_cli.msg_debug_on_timeout";
    String MOM_CLI_ROUTEES_NB_PER_SERVICE = "mom_cli.nb_routees_per_service";
    String MOM_CLI_RPC_TIMEOUT            = "mom_cli.rpc_timeout";
    String MOM_CLI_RPC_RETRY              = "mom_cli.rpc_retry";

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

    /**
     * @return MomClient ID which is defined in the MomClient configuration properties
     */
    String getClientID();

    /**
     * Init this MomClient thanks input properties
     * @param properties
     * @throws Exception if any configuration is missing
     */
    void   init(Properties properties) throws Exception;

    /**
     * Init this MomClient thanks input properties
     * @param properties
     * @throws Exception if any configuration is missing
     */
    void   init(Dictionary properties) throws Exception;

    /**
     * Close this MomClient and all requests executors/services attached to it
     * @throws Exception if any problems occurs
     */
    void   close() throws Exception;

    /**
     * @return the underlying MoM provider connection
     */
    Object getConnection();

    /**
     * @return true if connected else false
     */
    boolean isConnected();

    /**
     * Create a new request executor and attach it to this MomClient.
     * @return the fresh create request executor
     */
    MomRequestExecutor createRequestExecutor();

    /**
     * Get remote procedure call timeout.
     * @return
     */
    long getRPCTimout();

    /**
     * Set remote procedure call timeout.
     * @param rpcTimout
     */
    void setRPCTimout(long rpcTimout);

    /**
     * Get remote procedure call retry count.
     * @return
     */
    int getRPCRetry();

    /**
     * Set remote procedure call retry count.
     * @param rpcRetry
     */
    void setRPCRetry(int rpcRetry);


    /**
     * Open a new message group request and attach it to this MomClient.
     * @param groupID the message group request ID (must be unique)
     */
    void openMsgGroupRequest(String groupID);

    /**
     * Get the current message group request.
     * @return the message group request ID.
     */
    String getCurrentMsgGroup();

    /**
     * Close the specified message group request and dettach it from this MomClient.
     * @param groupID the message group ID
     */
    void closeMsgGroupRequest(String groupID);

    /**
     * @return attached Service Factory
     */
    MomServiceFactory getServiceFactory();

    /**
     * Open a new message group service dedicated to message group request groupID treatment and attach it to this
     * MomClient
     * @param groupID the message group request ID
     */
    void openMsgGroupServices(String groupID);

    /**
     * Close the message group service dedicated to message group request groupID treatment and dettach it from this
     * MomClient
     * @param groupID
     */
    void closeMsgGroupServices(String groupID);

    /**
     * @return false if message debug on timeout is configured else false
     */
    boolean isMsgDebugOnTimeout();

    /**
     * Set the message on debug timeout conf
     * @param msgDebugOnTimeout
     */
    void setMsgDebugOnTimeout(boolean msgDebugOnTimeout);
}