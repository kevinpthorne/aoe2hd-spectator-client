package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.config.AppStatus;

/**
 * Created by kevint on 1/20/2017.
 */
public interface Heartbeat {

    void updateStatus(AppStatus status);

    void setArguments(String[] args);

    void downstream(String gameId, String player);

}
