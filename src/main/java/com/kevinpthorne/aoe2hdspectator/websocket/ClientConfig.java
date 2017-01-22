package com.kevinpthorne.aoe2hdspectator.websocket;

import javax.websocket.ClientEndpointConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Created by kevint on 1/11/2017.
 */
public class ClientConfig extends ClientEndpointConfig.Configurator {

    static volatile boolean called = false;

    @Override
    public void beforeRequest(Map<String, List<String>> headers) {
        headers.put("Origin", Arrays.asList("127.0.0.1"));
        super.beforeRequest(headers);
    }
}
