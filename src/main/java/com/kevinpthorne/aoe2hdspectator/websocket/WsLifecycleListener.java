package com.kevinpthorne.aoe2hdspectator.websocket;

import javax.websocket.CloseReason;
import javax.websocket.Session;

/**
 * Created by kevint on 1/15/2017.
 */
public interface WsLifecycleListener {

    void onOpen(Session userSession);

    void onClose(Session userSession, CloseReason reason);

    void onError(Throwable e);

}
