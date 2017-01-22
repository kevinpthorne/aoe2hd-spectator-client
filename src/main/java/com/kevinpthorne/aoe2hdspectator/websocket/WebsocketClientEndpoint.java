package com.kevinpthorne.aoe2hdspectator.websocket;

import javax.websocket.ClientEndpoint;
import javax.websocket.CloseReason;
import javax.websocket.ContainerProvider;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by kevint on 1/11/2017.
 */
@ClientEndpoint(configurator = ClientConfig.class)
public class WebsocketClientEndpoint implements WsLifecycleListener {

    Session userSession = null;
    private MessageHandler messageHandler;
    private Set<WsLifecycleListener> lifecycleListeners = new HashSet<>();

    public double sent = 0;
    public double received = 0;

    public WebsocketClientEndpoint(URI endpointURI) {
        try {
            WebSocketContainer container = ContainerProvider.getWebSocketContainer();
            //container.setDefaultMaxBinaryMessageBufferSize(8192);
            container.connectToServer(this, endpointURI);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Callback hook for Connection open events.
     *
     * @param userSession the userSession which is opened.
     */
    @OnOpen
    public void onOpen(Session userSession) {
        System.out.println("opening websocket");
        this.userSession = userSession;
        for(WsLifecycleListener listener : lifecycleListeners) {
            listener.onOpen(userSession);
        }
    }

    /**
     * Callback hook for Connection close events.
     *
     * @param userSession the userSession which is getting closed.
     * @param reason      the reason for connection close
     */
    @OnClose
    public void onClose(Session userSession, CloseReason reason) {
        System.out.println("Closing websocket, sent " + (sent / 1024D) + "kb, recv " + (received / 1024D) + "kb");
        this.userSession = null;
        for(WsLifecycleListener listener : lifecycleListeners) {
            listener.onClose(userSession, reason);
        }
    }

    /**
     * Callback hook for Message Events. This method will be invoked when a client send a message.
     *
     * @param message The text message
     */
    @OnMessage
    public void onMessage(String message, Session session) {
        if (this.messageHandler != null) {
            this.messageHandler.handleText(message, session);
            received += message.getBytes().length;
        }
    }

    @OnMessage
    public void onBinary(byte[] data, Session session) {
        if (this.messageHandler != null) {
            this.messageHandler.handleBinary(data, session);
            received += data.length;
        }
    }

    @OnError
    public void onError(Throwable e) {
        e.printStackTrace();
        for(WsLifecycleListener listener : lifecycleListeners) {
            listener.onError(e);
        }
    }

    /**
     * register message handler
     *
     * @param msgHandler
     */
    public void addMessageHandler(MessageHandler msgHandler) {
        this.messageHandler = msgHandler;
    }

    public void addLifecycleListener(WsLifecycleListener listener) {
        this.lifecycleListeners.add(listener);
    }

    /**
     * Send a message.
     *
     * @param message
     */
    public void sendText(String message) {
        this.userSession.getAsyncRemote().sendText(message);
        sent += message.getBytes().length;
    }

    public void sendBinary(ByteBuffer bytes) {
        try {
            this.userSession.getBasicRemote().sendBinary(bytes);
            sent += bytes.array().length;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Session getUserSession() {
        return userSession;
    }

    /**
     * Message handler.
     *
     * @author Jiji_Sasidharan
     */
    public interface MessageHandler {

        void handleText(String message, Session session);

        void handleBinary(byte[] data, Session session);
    }
}
