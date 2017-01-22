package com.kevinpthorne.aoe2hdspectator.config;

import java.net.URL;

/**
 * Created by kevint on 1/19/2017.
 */
public enum AppStatus {

    STARTING("starting.png"),
    READY("ready.png"),
    ERROR("error.png"),
    WAITING("waiting.png"),
    UPSTREAMING("streaming.png"),
    DOWNSTREAMING("streaming.png");

    String icon;

    AppStatus(String icon) {
        this.icon = icon;
    }

    public String getIcon() {
        return icon;
    }

    public URL getIconResource() {
        return AppStatus.class.getResource("/icons/" + getIcon());
    }
}
