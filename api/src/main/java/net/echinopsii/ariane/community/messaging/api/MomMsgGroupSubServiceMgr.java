package net.echinopsii.ariane.community.messaging.api;

public interface MomMsgGroupSubServiceMgr {
    void openMsgGroupSubService(String groupID);
    void closeMsgGroupSubService(String groupID);
    void stop();
}
