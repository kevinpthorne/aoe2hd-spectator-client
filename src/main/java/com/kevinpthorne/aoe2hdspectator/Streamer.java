package com.kevinpthorne.aoe2hdspectator;

import com.eclipsesource.json.Json;
import com.eclipsesource.json.JsonObject;
import com.kevinpthorne.aoe2hdspectator.config.AppStatus;
import com.kevinpthorne.aoe2hdspectator.config.Config;
import com.kevinpthorne.aoe2hdspectator.websocket.WebsocketClientEndpoint;
import com.kevinpthorne.aoe2hdspectator.websocket.WsLifecycleListener;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by kevint on 1/5/2017.
 */
public class Streamer implements Runnable {

    private static final int UPLOAD_ERROR_CAP = 10;
    private static final String CHECKSUM_ALGORITHM = "sha1";

    private Logger log = StreamingApp.log;
    private Heartbeat master;
    private final Config config;

    private final boolean upstreaming;
    private Path uploading;

    private String gameToReceive;
    private String playerToRequest;

    private volatile boolean running;

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
     * @param gameId
     * @param player
     * @param config
     */
    public Streamer(String gameId, String player, Config config, Heartbeat master) {
        this.gameToReceive = gameId;
        this.playerToRequest = player;

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
            master.updateStatus(AppStatus.ERROR);
            running = false;
        }
        //}
    }

    private void upstream() throws IOException, InterruptedException, URISyntaxException {
        final WebsocketClientEndpoint client =
                buildUpstream(config.getRelayServer(), uploading.getFileName().toString(), config.getKey());

        master.updateStatus(AppStatus.UPSTREAMING);

        client.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                if (message.contains("error")) {
                    try {
                        client.getUserSession().close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else {
                    if (message.substring(0, 1).equalsIgnoreCase("g")) {
                        log.info("Game ID: " + message);
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
                    if (errors > 2) {
                        System.out.println("\nBuffering... [Attempt " + errors + "/" + UPLOAD_ERROR_CAP + "]");
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
                master.updateStatus(AppStatus.UPSTREAMING);
            }

            @Override
            public void onClose(Session userSession, CloseReason reason) {
                running = false;
                master.updateStatus(AppStatus.READY);
            }

            @Override
            public void onError(Throwable e) {
                master.updateStatus(AppStatus.ERROR);
            }
        });
        client.getUserSession().close();
    }

    private void downstream() throws UnsupportedEncodingException, URISyntaxException {
        final WebsocketClientEndpoint client =
                buildDownstream(config.getRelayServer(), gameToReceive, playerToRequest);

        int maxLogWidth = 50;
        final int[] currentWidth = {0};

        final int[] attempts = {0};
        int maxAttempts = 50;

        client.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                JsonObject json = Json.parse(message).asObject();
                if (json.get("status") == null && json.get("action") != null
                        && json.get("action").asString().equalsIgnoreCase("continue")
                        && json.get("position") != null) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    client.sendText("{\"action\":\"pull\",\"position\":" + json.get("position").asInt() + "}");

                } else if (json.get("status") == null && json.get("action") != null
                        && json.get("action").asString().equalsIgnoreCase("checksum")) {
                    try {
                        String serverChecksum = json.get("value").asString();
                        String clientChecksum = getChecksum(config.getSaveGameDirectory() + "/" + config.getReceiveFilename() + ".aoe2record", CHECKSUM_ALGORITHM);

                        if (serverChecksum.equals(clientChecksum)) {
                            System.out.println("File is good");
                        } else {
                            client.onError(new Exception("Checksum mismatch"));
                        }
                        client.getUserSession().close();
                    } catch (Exception e) {
                        System.err.println("\n\n");
                        e.printStackTrace();
                        try {
                            client.getUserSession().close();
                        } catch (IOException ignored) {
                        }
                    }
                } else {
                    String status = json.get("status").asString();

                    if (status.equalsIgnoreCase("error")) {
                        System.out.println("Error, pos: "
                                + (json.get("position") != null ? json.get("position").asString() : "?"));
                        try {
                            client.getUserSession().close();
                        } catch (IOException e) {
                            e.printStackTrace();
                            client.onError(e);
                        }
                    } else if (status.equalsIgnoreCase("eof")) {
                        if (json.get("position") != null && ++attempts[0] <= maxAttempts) {
                            currentWidth[0] = printProgress(true, maxLogWidth, currentWidth[0]);
                            try {
                                Thread.sleep(2000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                                client.onError(e);
                            }
                            client.sendText("{\"action\":\"pull\",\"position\":" + json.get("position").asInt() + "}");
                        } else {
                            //give up
                            client.sendText("{\"action\":\"" + CHECKSUM_ALGORITHM + "\"}");
                            try {
                                client.getUserSession().close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }

            @Override
            public void handleBinary(byte[] data, Session session) {
                try {
                    try { //TODO fix
                        Files.write(Paths.get(config.getSaveGameDirectory(), config.getReceiveFilename() + ".aoe2record"), data, StandardOpenOption.CREATE_NEW);
                    } catch (FileAlreadyExistsException e) {
                        Files.write(Paths.get(config.getSaveGameDirectory(), config.getReceiveFilename() + ".aoe2record"), data, StandardOpenOption.APPEND);
                    }
                    currentWidth[0] = printProgress(false, maxLogWidth, currentWidth[0]);
                } catch (IOException e) {
                    e.printStackTrace();
                    client.sendText("error");
                    client.onError(e);
                }
            }
        });

        client.addLifecycleListener(new WsLifecycleListener() {
            @Override
            public void onOpen(Session userSession) {
                if (config.isAutoLaunch())
                    try {
                        Desktop.getDesktop().browse(new URI("steam://rungameid/221380"));
                    } catch (URISyntaxException | IOException e) {
                        log.severe("Autolaunch failed");
                        e.printStackTrace();
                    }
            }

            @Override
            public void onClose(Session userSession, CloseReason reason) {
                try {
                    Files.move(Paths.get(config.getSaveGameDirectory(), config.getReceiveFilename() + ".aoe2record"),
                            Paths.get(config.getSaveGameDirectory(), playerToRequest + "." + gameToReceive + ".aoe2record"),
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                master.updateStatus(AppStatus.READY);
            }

            @Override
            public void onError(Throwable e) {
                try {
                    Files.move(Paths.get(config.getSaveGameDirectory(), config.getReceiveFilename() + ".aoe2record"),
                            Paths.get(config.getSaveGameDirectory(), playerToRequest + "." + gameToReceive + ".error.aoe2record"),
                            StandardCopyOption.ATOMIC_MOVE);
                } catch (IOException ignored) {
                }
                master.updateStatus(AppStatus.ERROR);
            }
        });

        client.sendText("{\"action\":\"pull\",\"position\":0}");
        client.onOpen(client.getUserSession());
    }

    private int printProgress(boolean isWait, int maxWidth, int currentWidth) {
        if (currentWidth < maxWidth) {
            System.out.print(isWait ? "#" : ".");
            currentWidth++;
        } else {
            System.out.println(isWait ? "#" : ".");
            currentWidth = 0;
        }
        return currentWidth;
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

    private static WebsocketClientEndpoint buildStream(String server, String filename, String identifierType,
                                                       String identifier, String command)
            throws UnsupportedEncodingException, URISyntaxException {
        Map<String, String> queries = new HashMap<>();
        queries.put("filename", String.valueOf(filename));
        queries.put(identifierType, identifier);
        return buildClient(server, command, queries);
    }

    private static WebsocketClientEndpoint buildDownstream(String server, String filename, String player)
            throws UnsupportedEncodingException, URISyntaxException {
        return buildStream(server, filename, "player", player, "downstream");
    }

    private static WebsocketClientEndpoint buildUpstream(String server, String filename, String key)
            throws UnsupportedEncodingException, URISyntaxException {
        return buildStream(server, filename, "key", key, "upstream");
    }

    private static byte[] createChecksum(String filename, String algorithm) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance(algorithm.toUpperCase());
        int numRead;

        do {
            numRead = fis.read(buffer);
            if (numRead > 0) {
                complete.update(buffer, 0, numRead);
            }
        } while (numRead != -1);

        fis.close();
        return complete.digest();
    }

    private static String getChecksum(String filename, String algorithm) throws Exception {
        byte[] b = createChecksum(filename, algorithm);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }
}
