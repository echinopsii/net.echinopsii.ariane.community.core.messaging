/**
 * Messaging - Common Implementation
 * Application High Payload Message Service Worker abstract implementation
 * Copyright (C) 12/27/16 echinopsii
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

import net.echinopsii.ariane.community.messaging.api.AppMsgWorker;
import net.echinopsii.ariane.community.messaging.api.MomMsgTranslator;
import net.echinopsii.ariane.community.messaging.api.MomService;
import net.echinopsii.ariane.community.messaging.api.MomServiceFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * MomAkkaAbsAppHPMsgSrvWorker provides an abstract implementation of AppMsgWorker interface based on actors model and Akka
 * for services needing to manage high payload messages which could be splitted in severals messages depending the message
 * max payload size of your message broker.
 */
public abstract class MomAkkaAbsAppHPMsgSrvWorker implements AppMsgWorker {

    private static HashMap<String, MomService> splitMsgGroupServices = new HashMap<>();
    private MomServiceFactory serviceFactory = null;

    public HashMap<String, Integer> wipMsgCount = new HashMap<>();
    public HashMap<String, Object[]> wipMsg = new HashMap<>();

    public MomAkkaAbsAppHPMsgSrvWorker(MomServiceFactory serviceFactory_) {
        this.serviceFactory = serviceFactory_;
    }

    @Override
    public Map<String, Object> apply(Map<String, Object> message) {
        if (this.serviceFactory==null) return null;

        Map<String, Object> reply = new HashMap<>();
        if (message.containsKey(MomMsgTranslator.OPERATION_FDN)) {
            String op = (String) message.get(MomMsgTranslator.OPERATION_FDN);
            String msgSplitGroupID = null;
            String msgSplitDest = null;
            switch (op) {
                case MomMsgTranslator.OP_MSG_SPLIT_FEED_INIT:
                    msgSplitGroupID = (String) message.get(MomMsgTranslator.PARAM_MSG_SPLIT_MID);
                    msgSplitDest = (String) message.get(MomMsgTranslator.PARAM_MSG_SPLIT_FEED_DEST);
                    if (msgSplitGroupID!=null && msgSplitDest!=null) {
                        splitMsgGroupServices.put(msgSplitGroupID, this.serviceFactory.requestService(msgSplitDest, this));
                        reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_SUCCESS);
                        reply.put(MomMsgTranslator.MSG_BODY, "INITIALISED");
                    } else {
                        reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_BAD_REQ);
                        reply.put(MomMsgTranslator.MSG_BODY, "Fields " + MomMsgTranslator.PARAM_MSG_SPLIT_MID + " and/or " + MomMsgTranslator.PARAM_MSG_SPLIT_FEED_DEST + " are not properly defined !");
                    }
                    break;
                case MomMsgTranslator.OP_MSG_SPLIT_FEED_END:
                    msgSplitGroupID = (String) message.get(MomMsgTranslator.PARAM_MSG_SPLIT_MID);
                    if (msgSplitGroupID!=null) {
                        if (splitMsgGroupServices.containsKey(msgSplitGroupID)) {
                            splitMsgGroupServices.get(msgSplitGroupID).stop();
                            splitMsgGroupServices.remove(msgSplitGroupID);
                            reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_SUCCESS);
                            reply.put(MomMsgTranslator.MSG_BODY, "ENDED");
                        } else {
                            reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_NOT_FOUND);
                            reply.put(MomMsgTranslator.MSG_BODY, "Message split worker not found for id " + msgSplitGroupID + " !");
                        }
                    } else {
                        reply.put(MomMsgTranslator.MSG_RC, MomMsgTranslator.MSG_RET_BAD_REQ);
                        reply.put(MomMsgTranslator.MSG_BODY, "Field " + MomMsgTranslator.PARAM_MSG_SPLIT_MID + " is not properly defined !");
                    }
                    break;
                default:
                    break;
            }
        }
        return (reply.size()>0) ? reply : null;
    }


}
