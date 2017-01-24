package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.websocket.WsLifecycleListener;
import com.kevinpthorne.aoe2hdspectator.websocket.WebsocketClientEndpoint;

import javax.websocket.CloseReason;
import javax.websocket.Session;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * Created by kevint on 1/11/2017.
 */
public class ManualTestWebSocket {

    public static void main(String[] args) throws Exception {

        System.setProperty("java.net.preferIPv4Stack" , "true");

        String host = "localhost:8082";
        if (args.length < 3) {
            if(args.length <= 1) {
                String command = args[0];
                if(args.length == 2) {
                    host = args[1];
                }
                if(command.equalsIgnoreCase("echo")) {
                    echo(host);
                    return;
                }
                else if(command.equalsIgnoreCase("becho")) {
                    becho(host);
                    return;
                }
            }
            System.err.println("Expected more arguments");
            //TODO print help
        } else if (args.length == 3 || args.length == 4) {
            String command = args[0];
            String file = args[1];
            String player = args[2];

            if (args.length == 4) {
                host = args[3];
            }

            if (!file.contains(".aoe2record")) {
                System.err.println("Invalid file");
                //TODO print help
            }

            if (command.equalsIgnoreCase("upstream")) {
                //upstream
                if (!new File(file).exists()) {
                    System.err.println("File not found");
                    //TODO print help
                }
                System.out.println("-- Upstreaming " + file + " of " + player);
                upstream(file, player, host);
                //becho();
            } else if (command.equalsIgnoreCase("downstream")) {
                //downstream
                System.out.println("-- Downstreaming " + file + " of " + player);
                downstream(file, player, host);
            } else {
                System.err.println("Invalid Command");
                //TODO print help
            }
        } else {
            System.err.println("Too many arguments");
            //TODO print help
        }

    }

    private static void echo(String server) throws URISyntaxException, InterruptedException, IOException {
        // open websocket
        final WebsocketClientEndpoint clientEndPoint =
                new WebsocketClientEndpoint(new URI("ws://" + server + "/test/echo"));

        // add listener
        clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                System.out.println("Received:\t\"" + message + "\"");
            }

            @Override
            public void handleBinary(byte[] data, Session session) {
                //pass
            }
        });

        for (int i = 0; i < 5; i++) {
            System.out.println("Sending:\t\"test message " + i + "\"");
            clientEndPoint.sendText("test message " + i);
            Thread.sleep(1000);
        }

        clientEndPoint.getUserSession().close();
    }

    private static void becho(String server) throws URISyntaxException, InterruptedException, IOException {
        System.out.println("initiating connection");
        // open websocket
        final WebsocketClientEndpoint clientEndPoint =
                new WebsocketClientEndpoint(new URI("ws://" + server + "/test/becho"));

        System.out.println("connection made");

        // add listener
        clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                //pass
            }

            @Override
            public void handleBinary(byte[] data, Session session) {
                System.out.println("Received:\t\"" + new String(data) + "\"");
            }
        });

        for (int i = 0; i < 5; i++) {
            System.out.println("Sending:\t 5 bytes");
            clientEndPoint.sendBinary(ByteBuffer.wrap(new byte[5]));
            Thread.sleep(1000);
        }

        clientEndPoint.getUserSession().close();
    }

    private static void upstream(String file, String player, String server) throws IOException, URISyntaxException, InterruptedException {
        // open websocket
        final WebsocketClientEndpoint clientEndPoint =
                new WebsocketClientEndpoint(new URI("ws://" + server + "/upstream?filename=" +
                        URLEncoder.encode(new File(file).getName(), "UTF-8") +
                        "&player=" +
                        URLEncoder.encode(player, "UTF-8")));

        // add listener
        clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                if (message.contains("error")) {
                    try {
                        clientEndPoint.getUserSession().close();
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


        FileInputStream ios = new FileInputStream(new File(file));
        int length = -1;
        int position = 0;
        int errors = 0;
        byte[] buffer;// buffer for portion of data from connection
        byte[] data;
        synchronized (ios) {
            while (errors < 5) {
                if (position < (256 * 1024)) { //mgz header information
                    buffer = new byte[256 * 1024];
                } else {
                    buffer = new byte[4096];
                }
                if ((length = ios.read(buffer)) <= -1) {
                    System.out.println("Buffering... [Attempt " + ++errors + "/5]");
                    //Thread.sleep(1000);
                    ios.wait(1000);
                } else {
                    data = Arrays.copyOf(buffer, length);
                    clientEndPoint.sendBinary(ByteBuffer.wrap(data));
                    position += length;
                    errors = 0;

                    ios.wait(10);

                    //Thread.sleep(10);
                }
            }
        }

        clientEndPoint.getUserSession().close();
        System.exit(0);
    }

    private static void downstream(String file, String player, String server) throws IOException, URISyntaxException, InterruptedException {
        // open websocket
        final WebsocketClientEndpoint clientEndPoint =
                new WebsocketClientEndpoint(new URI("ws://" + server + "/downstream?filename=" +
                        URLEncoder.encode(new File(file).getName(), "UTF-8") +
                        "&player=" +
                        URLEncoder.encode(player, "UTF-8")));

        // add listener
        clientEndPoint.addMessageHandler(new WebsocketClientEndpoint.MessageHandler() {
            @Override
            public void handleText(String message, Session session) {
                if (message.contains("error")) {
                    try {
                        clientEndPoint.getUserSession().close();
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
                        Files.write(Paths.get(file + ".aoe2record"), data, StandardOpenOption.CREATE_NEW);
                    } catch (FileAlreadyExistsException e) {
                        Files.write(Paths.get(file + ".aoe2record"), data, StandardOpenOption.APPEND);
                    }
                    System.out.print(".");
                    //System.out.println("\t" + clientEndPoint.received);
                } catch (IOException e) {
                    e.printStackTrace();
                    clientEndPoint.sendText("error");
                }
            }
        });
        System.out.println(clientEndPoint.getUserSession().getRequestURI().toASCIIString());

        clientEndPoint.addLifecycleListener(new WsLifecycleListener() {
            @Override
            public void onOpen(Session userSession) {
                //pass
            }

            @Override
            public void onClose(Session userSession, CloseReason reason) {
                try {
                    System.out.println(getMD5Checksum(file));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.exit(0);
            }

            @Override
            public void onError(Throwable e) {
                //pass
            }
        });

        clientEndPoint.sendText("start");
    }

    private static byte[] createChecksum(String filename) throws Exception {
        InputStream fis = new FileInputStream(filename);

        byte[] buffer = new byte[1024];
        MessageDigest complete = MessageDigest.getInstance("MD5");
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

    private static String getMD5Checksum(String filename) throws Exception {
        byte[] b = createChecksum(filename);
        String result = "";

        for (int i = 0; i < b.length; i++) {
            result += Integer.toString((b[i] & 0xff) + 0x100, 16).substring(1);
        }
        return result;
    }

}
