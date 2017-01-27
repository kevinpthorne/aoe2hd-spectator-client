package com.kevinpthorne.aoe2hdspectator.io;

import com.kevinpthorne.aoe2hdspectator.Heartbeat;
import com.kevinpthorne.aoe2hdspectator.StreamingApp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by kevint on 1/26/2017.
 */
public class URIHandler implements Runnable {

    private static final int PORT = 49160;

    private static final String PROTOCOL = "aoe2hdspectator://";
    private static final String VALID_COMMAND = PROTOCOL + "downstream";
    private static final String URI_RECEIVED_REPLY = "HIT";

    private volatile boolean running;

    private final Heartbeat master;

    public URIHandler(Heartbeat master) {
        this.master = master;
    }

    @Override
    public void run() {
        running = true;
        try {
            DatagramSocket serverSocket = new DatagramSocket(PORT, InetAddress.getLoopbackAddress());
            byte[] receiveData = new byte[1024];

            System.out.printf("Listening on udp:%s:%d%n",
                    InetAddress.getLoopbackAddress(), PORT);
            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);

            while (running) {
                serverSocket.receive(receivePacket);
                String uri = new String(receivePacket.getData(), 0,
                        receivePacket.getLength());
                // now send acknowledgement packet back to sender
                System.out.println("RECEIVED: " + uri);
                InetAddress source = receivePacket.getAddress();
                byte[] sendData = URI_RECEIVED_REPLY.getBytes("UTF-8");
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length,
                        source, receivePacket.getPort());
                serverSocket.send(sendPacket);

                validateAndTrigger(uri, false);
            }
        } catch (IOException e) {
            StreamingApp.log.severe("URIHandler died: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateAndTrigger(String uri, boolean queue) {
        //validate uri
        try {
            String[] trigger = validateURI(uri);
            if (queue)
                queueDownstream(trigger);
            else
                startDownstream(trigger);
        } catch (MalformedURLException e) {
            StreamingApp.log.warning(e.getMessage() + ": " + uri);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    private void startDownstream(String[] trigger) {
        master.downstream(trigger[0], trigger[1]);
    }

    private void queueDownstream(String[] trigger) {
        master.setArguments(trigger);
    }

    private String[] validateURI(String uri) throws MalformedURLException, UnsupportedEncodingException {
        String[] result = {"", ""}; //gameid, player
        Pattern pattern = Pattern.compile("(((aoe2hdspectator:\\/\\/)[A-Za-z0-9.-]+|[A-Za-z0-9.-]+)((?:\\/[\\+~%\\/.\\w-_]*)?\\??(?:[-\\+=&;%@.\\w_]*)#?(?:[\\w]*))?)");
        Matcher matcher = pattern.matcher(uri);
        String protocol = "";
        String command = "";
        String request = null;

        while (matcher.find()) {
            command = matcher.group(2);
            protocol = matcher.group(3);
            request = matcher.group(4);
        }

        if (protocol.equals(PROTOCOL)) {
            if (command.equals(VALID_COMMAND)) {
                if (request != null && request.charAt(0) == '/') {
                    request = request.substring(1, request.length()); //cut off /
                    String[] split = request.split("/");
                    result[0] = URLDecoder.decode(split[0], "UTF-8");
                    result[1] = URLDecoder.decode(split[1], "UTF-8");
                }
            }
        }

        if (result[0].isEmpty() || result[1].isEmpty()) {
            throw new MalformedURLException("Bad URI");
        }

        return result;
    }

    public void sendUri(String uri) throws IOException {
        byte[] data = uri.getBytes();
        InetAddress address = InetAddress.getLoopbackAddress();
        DatagramPacket packet = new DatagramPacket(
                data, data.length, address, PORT
        );
        DatagramSocket datagramSocket = new DatagramSocket();
        datagramSocket.send(packet);

        awaitHit(datagramSocket, uri);
    }

    private void awaitHit(DatagramSocket socket, String uri) {
        running = true;
        try {
            socket.setSoTimeout(100);
            byte[] receiveData = new byte[8];

            DatagramPacket receivePacket = new DatagramPacket(receiveData,
                    receiveData.length);
            socket.receive(receivePacket);
            String reply = new String(receivePacket.getData(), 0,
                    receivePacket.getLength());
            if (reply.equalsIgnoreCase(URI_RECEIVED_REPLY)) {
                running = false;
                System.exit(0);
            }
        } catch (IOException e) {
            if (!e.getMessage().contains("Receive timed out")) {
                e.printStackTrace();
                System.exit(1);
            } else {
                validateAndTrigger(uri, true);
            }
        }
    }

    public void stop() {
        running = false;
    }
}
