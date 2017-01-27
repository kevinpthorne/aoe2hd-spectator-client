package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.config.AppStatus;
import com.kevinpthorne.aoe2hdspectator.io.Language;
import com.kevinpthorne.aoe2hdspectator.io.ConfigLoader;
import com.kevinpthorne.aoe2hdspectator.io.URIHandler;

import javax.swing.*;
import java.awt.AWTException;
import java.awt.Desktop;
import java.awt.TrayIcon;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by kevint on 1/19/2017.
 */
public class StreamingApp implements StreamingTray.TrayListener, Heartbeat {

    private Thread streamerThread;
    private Streamer streamer;

    private StreamingTray tray;
    private ConfigLoader configLoader;
    private Language language;

    private Thread uriHandlerThread;
    private URIHandler uriHandler;
    private String[] args;

    static AppStatus status;

    public static Logger log = Logger.getLogger("AoE2HD-Streamer");

    public static void main(String[] args) throws Exception {
        new StreamingApp(args);
    }

    private StreamingApp(String[] args) {
        this.args = args;
        log.info("Starting AoE2HD Streaming app...");
        status = AppStatus.STARTING;

        try {
            FileHandler fh = new FileHandler("aoe2hdspectator.log", true);
            log.addHandler(fh);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);

            uriHandler = new URIHandler(this);
            if (args.length == 1) {
                uriHandler.sendUri(args[0]);
            }
            uriHandlerThread = new Thread(uriHandler);
            uriHandlerThread.start();

            configLoader = new ConfigLoader();
            language = new Language(configLoader.getConfig().getLanguage());

            tray = new StreamingTray(this, configLoader.getConfig(), language);

            log.info("Ready");
            status = AppStatus.READY;
            tray.updateStatus();

            if(this.args.length == 2) {
                downstream(this.args[0], this.args[1]);
            }
        } catch (IOException e) {
            status = AppStatus.ERROR;
            if (tray != null) {
                tray.updateStatus();
            }
            e.printStackTrace();
        } catch (AWTException e) {
            status = AppStatus.ERROR;
            e.printStackTrace();
        }
    }

    public void updateStatus(AppStatus status) {
        System.out.println(status);
        StreamingApp.status = status;
        tray.updateStatus();
    }

    public void setArguments(String[] args) {
        this.args = args;
    }

    public void downstream(String gameId, String player) {
        log.info("Downstreaming " + gameId + " - " + player);
        streamer = new Streamer(gameId, player,
                configLoader.getConfig(),
                StreamingApp.this);
        streamerThread = new Thread(streamer);
        streamerThread.start();
    }

    @Override
    public void onUpStream() {
        SwingUtilities.invokeLater(() -> {
            final Path path = FileSystems.getDefault().getPath(configLoader.getConfig().getSaveGameDirectory());
            System.out.println(path);
            try (final WatchService watchService = FileSystems.getDefault().newWatchService()) {
                final WatchKey watchKey = path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

                if (configLoader.getConfig().isUpstreamNotifications()) {
                    tray.pushNotification(language.getString("notification_upstream_title"),
                            language.getString("status_upstream_waiting"),
                            TrayIcon.MessageType.INFO);
                }
                updateStatus(AppStatus.WAITING);

                boolean watching = true;
                while (watching) {
                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        final Path changed = (Path) event.context();
                        //todo setup upstream thread
                        if (changed.toString().contains("aoe2record")) {
                            streamer = new Streamer(
                                    Paths.get(configLoader.getConfig().getSaveGameDirectory(), changed.toString()),
                                    configLoader.getConfig(),
                                    StreamingApp.this);
                            streamerThread = new Thread(streamer);
                            streamerThread.start();
                            watching = false;
                            break;
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if (!valid) {
                        log.warning("Key has been unregistered");
                    }
                }
                watchService.close();
            } catch (IOException e) {
                updateStatus(AppStatus.ERROR);
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });

    }

    @Override
    public void onDownStream() {
        try {
            Desktop.getDesktop().browse(new URI("http://" +
                    configLoader.getConfig().getRelayServer().
                            substring(0, configLoader.getConfig().getRelayServer().indexOf(":"))));
        } catch (IOException | URISyntaxException e) {
            updateStatus(AppStatus.ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void onConfig() {
        try {
            Desktop.getDesktop().open(new File(ConfigLoader.CONFIG_FILENAME));
        } catch (IOException e) {
            updateStatus(AppStatus.ERROR);
            e.printStackTrace();
        }
    }

    @Override
    public void onExit() {
        if (streamer != null) {
            log.warning("Killing streamer");
        }
        System.exit(0);
    }


}
