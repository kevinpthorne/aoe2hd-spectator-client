package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.config.AppStatus;
import com.kevinpthorne.aoe2hdspectator.config.Config;
import com.kevinpthorne.aoe2hdspectator.websocket.WebsocketClientEndpoint;
import com.kevinpthorne.aoe2hdspectator.websocket.WsLifecycleListener;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by kevint on 1/5/2017.
 */
public class Streamer implements Runnable {

    private static final int UPLOAD_ERROR_CAP = 10;

    private Logger log = StreamingApp.log;
    private Heartbeat master;

    private final boolean upstreaming;
    private final Config config;

    private Path uploading;

    private String gameToReceive;
    private String playerToRequest;

    private WebsocketClientEndpoint client;
    private boolean running;

    /**
     * Upstreaming is assumed here
     *
     * @param filename
     * @param config
     */
    public Streamer(Path filename, Config config, Heartbeat master) {
        this.master = master;
        this.config = config;
        uploading = filename;
        upstreaming = true;
    }

    /**
     * Downstreaming is assumed here
     *
     * @param filename
     * @param player
     * @param config
     */
    public Streamer(String filename, String player, Config config, Heartbeat master) {
        this.master = master;
        this.config = config;
        upstreaming = false;
    }

    @Override
    public void run() {
        running = true;
        //while (running) {
            try {
                if (upstreaming) {
                    upstream();
                } else {
                    downstream();
                }
            } catch (InterruptedException | IOException | URISyntaxException e) {
                e.printStackTrace();
                running = false;
            }
        //}
        return;
    }

    private void upstream() throws IOException, InterruptedException, URISyntaxException {
        final WebsocketClientEndpoint client =
                buildUpstream(config.getRelayServer(), uploading.getFileName().toString(), config.getUsername());

        client.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                if (message.contains("error")) {
                    try {
                        client.getUserSession().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            @Override
            public void handleBinary(byte[] data, Session session) {
                //pass
            }
        });
        System.out.println(client.getUserSession().getRequestURI().toASCIIString());

        FileInputStream ios = new FileInputStream(uploading.toFile());
        int length = -1;
        int position = 0;
        int errors = 0;
        byte[] buffer;// buffer for portion of data from connection
        byte[] data;
        synchronized (ios) {
            while (errors < UPLOAD_ERROR_CAP) {
                if (position < (256 * 1024)) { //mgz header information
                    buffer = new byte[256 * 1024];
                } else {
                    buffer = new byte[4096];
                }
                if ((length = ios.read(buffer)) <= -1) {
                    ++errors;
                    if(errors > 2) {
                        System.out.println("\nBuffering... [Attempt " + errors +"/" + UPLOAD_ERROR_CAP + "]");
                    }
                    //Thread.sleep(1000);
                    ios.wait(1000);
                } else {
                    System.out.print(".");
                    data = Arrays.copyOf(buffer, length);
                    client.sendBinary(ByteBuffer.wrap(data));
                    position += length;
                    errors = 0;

                    ios.wait(10);
                }
            }
        }

        client.addLifecycleListener(new WsLifecycleListener() {
            @Override
            public void onOpen(Session userSession) {
                master.setStatus(AppStatus.UPSTREAMING);
            }

            @Override
            public void onClose(Session userSession, CloseReason reason) {
                running = false;
                master.setStatus(AppStatus.READY);
            }

            @Override
            public void onError(Throwable e) {
                master.setStatus(AppStatus.ERROR);
            }
        });
        client.getUserSession().close();
    }

    private void downstream() throws UnsupportedEncodingException, URISyntaxException {
        final WebsocketClientEndpoint client =
                buildDownstream(config.getRelayServer(), uploading.toAbsolutePath().toString(), config.getUsername());

        client.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                if (message.contains("error")) {
                    try {
                        client.getUserSession().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.println(message);
            }

            @Override
            public void handleBinary(byte[] data, Session session) {
                try {
                    try { //TODO fix
                        Files.write(Paths.get(config.getSaveGameDirectory(), config.getReceiveFilename() + ".aoe2record"), data, StandardOpenOption.CREATE_NEW);
                    } catch (FileAlreadyExistsException e) {
                        Files.write(Paths.get(config.getSaveGameDirectory(), config.getReceiveFilename() + ".aoe2record"), data, StandardOpenOption.APPEND);
                    }
                    System.out.print(".");
                    if (config.isAutoLaunch())
                        try {
                            Desktop.getDesktop().browse(new URI("steam://rungameid/221380"));
                        } catch (URISyntaxException e) {
                            log.severe("Autolaunch failed");
                            e.printStackTrace();
                        }
                } catch (IOException e) {
                    e.printStackTrace();
                    client.sendText("error");
                }
            }
        });

        client.addLifecycleListener(new WsLifecycleListener() {
            @Override
            public void onOpen(Session userSession) {
                //pass
            }

            @Override
            public void onClose(Session userSession, CloseReason reason) {
//                try {
//                    System.out.println(getMD5Checksum(file));
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                master.setStatus(AppStatus.READY);
            }

            @Override
            public void onError(Throwable e) {
                master.setStatus(AppStatus.ERROR);
            }
        });

        client.sendText("start");

    }

    private static WebsocketClientEndpoint buildClient(String server, String command, Map<String, String> queries)
            throws UnsupportedEncodingException, URISyntaxException {
        String path = "ws://" + server + "/" + command + "?";

        for (Map.Entry<String, String> query : queries.entrySet()) {
            path += URLEncoder.encode(query.getKey(), "UTF-8") + "=" +
                    URLEncoder.encode(query.getValue(), "UTF-8") + "&";
        }
        if (path.charAt(path.length() - 1) == '&') {
            path = path.substring(0, path.length() - 1);
        }

        // open websocket
        return new WebsocketClientEndpoint(new URI(path));
    }

    private static WebsocketClientEndpoint buildStream(String server, String filename, String player, String command)
            throws UnsupportedEncodingException, URISyntaxException {
        Map<String, String> queries = new HashMap<>();
        queries.put("filename", String.valueOf(filename));
        queries.put("player", player);
        return buildClient(server, command, queries);
    }

    private static WebsocketClientEndpoint buildDownstream(String server, String filename, String player)
            throws UnsupportedEncodingException, URISyntaxException {
        return buildStream(server, filename, player, "downstream");
    }

    private static WebsocketClientEndpoint buildUpstream(String server, String filename, String player)
            throws UnsupportedEncodingException, URISyntaxException {
        return buildStream(server, filename, player, "upstream");
    }
}
