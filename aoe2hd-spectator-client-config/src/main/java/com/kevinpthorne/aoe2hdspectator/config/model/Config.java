package com.kevinpthorne.aoe2hdspectator.config.model;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by kevint on 1/17/2017.
 */
public class Config {

    public static final String CONFIG_FILENAME = "config.ini";

    private final StringProperty language;

    private final StringProperty savegameDirectory;

    private final StringProperty key;

    private final StringProperty receiveFilename;
    private final StringProperty relayServer;

    private final BooleanProperty upstreamNotifications;
    private final BooleanProperty downstreamOverlay;
    private final BooleanProperty autoLaunch;

    public Config() {
        this("en", "", "", "game-spectating", "live.aoe2.net", false, true, true);
    }

    public Config(String language, String saveGameDir, String key, String receiveFilename,
                  String relayServer, boolean upstreamNotifications, boolean downstreamOverlay, boolean autoLaunch) {

        this.language = new SimpleStringProperty(language);

        this.savegameDirectory = new SimpleStringProperty(saveGameDir);

        this.key = new SimpleStringProperty(key);

        this.receiveFilename = new SimpleStringProperty(receiveFilename);
        this.relayServer = new SimpleStringProperty(relayServer);

        this.upstreamNotifications = new SimpleBooleanProperty(upstreamNotifications);
        this.downstreamOverlay = new SimpleBooleanProperty(downstreamOverlay);
        this.autoLaunch = new SimpleBooleanProperty(autoLaunch);

    }

    public static Config parse() throws IOException {
        Properties config = new Properties();
        Config result = null;
        try (InputStream input = new FileInputStream(CONFIG_FILENAME)) {
            config.load(input);

            return new Config(config.getProperty("language"),
                    config.getProperty("save_game_directory"),
                    config.getProperty("key"),
                    config.getProperty("receive_filename", "game-spectating"),
                    config.getProperty("relay_server"),
                    Boolean.parseBoolean(config.getProperty("upstream_notifications_enabled", "false")),
                    Boolean.parseBoolean(config.getProperty("downstream_overlay_enabled", "true")),
                    Boolean.parseBoolean(config.getProperty("downstream_auto_launch", "true")));
        }
    }

    public static void save(Config config) throws IOException {
        Properties file = new Properties();
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILENAME)) {
            file.setProperty("language", config.getLanguage());
            file.setProperty("save_game_directory", config.getSaveGameDirectory());
            file.setProperty("key", config.getKey());
            file.setProperty("receive_filename", config.getReceiveFilename());
            file.setProperty("relay_server", config.getRelayServer());
            file.setProperty("upstream_notifications_enabled", String.valueOf(config.isUpstreamNotifications()));
            file.setProperty("downstream_overlay_enabled", String.valueOf(config.isDownstreamOverlay()));
            file.setProperty("downstream_auto_launch", String.valueOf(config.isAutoLaunch()));

            file.store(output, null);

            System.out.println(output.getFD().toString());
        }
    }

    public String getLanguage() {
        return language.get();
    }

    public void setLanguage(String language) {
        this.language.set(language);
    }

    public String getSaveGameDirectory() {
        return savegameDirectory.get();
    }

    public void setSaveGameDirectory(String savegameDirectory) {
        this.savegameDirectory.set(savegameDirectory);
    }

    public String getKey() {
        return key.get();
    }

    public void setKey(String key) {
        this.key.set(key);
    }

    public String getRelayServer() {
        return relayServer.get();
    }

    public void setRelayServer(String relayServer) {
        this.relayServer.set(relayServer);
    }

    public String getReceiveFilename() {
        return receiveFilename.get();
    }

    public void setReceiveFilename(String filename) {
        receiveFilename.set(filename);
    }

    public boolean isUpstreamNotifications() {
        return upstreamNotifications.get();
    }

    public void setUpstreamNotifications(boolean enabled) {
        upstreamNotifications.setValue(enabled);
    }

    public boolean isDownstreamOverlay() {
        return downstreamOverlay.get();
    }

    public void setDownstreamOverlay(boolean enabled) {
        downstreamOverlay.setValue(enabled);
    }

    public boolean isAutoLaunch() {
        return autoLaunch.get();
    }

    public void setAutoLaunch(boolean enabled) {
        autoLaunch.setValue(enabled);
    }

}
