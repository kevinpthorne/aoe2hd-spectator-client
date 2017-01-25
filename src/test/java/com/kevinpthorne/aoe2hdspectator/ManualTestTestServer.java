package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.websocket.WebsocketClientEndpoint;
import com.kevinpthorne.aoe2hdspectator.websocket.WsLifecycleListener;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.net.URI;

/**
 * Created by kevint on 1/24/2017.
 */
public class ManualTestTestServer {

    public static void main(String[] args) throws Exception {

        // open websocket
        final WebsocketClientEndpoint client =
                new WebsocketClientEndpoint(new URI("ws://localhost:8082"));

        client.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                System.out.println("Received:\t\"" + message + "\"");
            }

            @Override
            public void handleBinary(byte[] data, Session session) {
                //pass
            }
        });

        client.addLifecycleListener(new WsLifecycleListener() {
            @Override
            public void onOpen(Session userSession) {
                System.out.println("Connected!");
            }

            @Override
            public void onClose(Session userSession, CloseReason reason) {
                System.out.println("Disconnected!");
                System.exit(0);
            }

            @Override
            public void onError(Throwable e) {
                //pass
            }
        });

        client.sendText("Do yo thang");

    }

}
