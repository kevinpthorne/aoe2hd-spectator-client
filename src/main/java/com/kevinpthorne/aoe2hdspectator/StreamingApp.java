package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.config.AppStatus;
import com.kevinpthorne.aoe2hdspectator.io.Language;
import com.kevinpthorne.aoe2hdspectator.io.ConfigLoader;

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
import java.util.logging.Logger;

/**
 * Created by kevint on 1/19/2017.
 */
public class StreamingApp implements StreamingTray.TrayListener, Heartbeat {

    private Streamer streamer;
    private StreamingTray tray;
    private ConfigLoader configLoader;
    private Language language;

    public static AppStatus status;

    public static Logger log = Logger.getLogger("AoE2HD-Streamer");

    public static void main(String[] args) throws Exception {
        new StreamingApp(args);
    }

    private StreamingApp(String[] args) {
        log.info("Starting AoE2HD Streaming app...");
        status = AppStatus.STARTING;
        if (args.length > 0) {
            log.info("Starting w/ arguments");
        }

        try {
            configLoader = new ConfigLoader();
            language = new Language(configLoader.getConfig().getLanguage());

            tray = new StreamingTray(this, configLoader.getConfig(), language);

            log.info("Ready");
            status = AppStatus.READY;
            tray.updateStatus();
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

                watchLoop:
                while (true) {
                    final WatchKey wk = watchService.take();
                    for (WatchEvent<?> event : wk.pollEvents()) {
                        final Path changed = (Path) event.context();
                        //todo setup upstream thread
                        if (changed.toString().contains("aoe2record")) {
                            streamer = new Streamer(
                                    Paths.get(configLoader.getConfig().getSaveGameDirectory(), changed.toString()),
                                    configLoader.getConfig(),
                                    StreamingApp.this);
                            streamer.run();
                            break watchLoop;
                        }
                    }
                    // reset the key
                    boolean valid = wk.reset();
                    if (!valid) {
                        log.warning("Key has been unregistered");
                    }
                }
                updateStatus(AppStatus.UPSTREAMING);
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

    @Override
    public void setStatus(AppStatus status) {
        updateStatus(status);
    }
}
