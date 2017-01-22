package com.kevinpthorne.aoe2hdspectator.io;

import com.kevinpthorne.aoe2hdspectator.config.Config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by kevint on 1/19/2017.
 */
public class ConfigLoader {

    public static final String CONFIG_FILENAME = "config.txt";

    private Properties map;

    private Config loadedConfig;

    public ConfigLoader() throws IOException {
        if (!new File(CONFIG_FILENAME).exists()) {
            saveDefaultConfig();
        }
        loadedConfig = loadConfig();
    }

    public Config getConfig() {
        return loadedConfig;
    }

    private Config loadConfig() throws IOException {
        this.map = new Properties();
        this.map.load(new FileInputStream(new File(CONFIG_FILENAME)));
        return parseConfig(this.map);
    }

    private Config parseConfig(Properties map) throws IOException {
        return new Config(map.getProperty("language"),
                map.getProperty("save_game_directory"),
                map.getProperty("username"),
                map.getProperty("password"),
                map.getProperty("receive_filename", "game-spectating"),
                map.getProperty("relay_server"),
                Boolean.parseBoolean(map.getProperty("upstream_notifications_enabled", "false")),
                Boolean.parseBoolean(map.getProperty("downstream_overlay_enabled", "true")),
                Boolean.parseBoolean(map.getProperty("downstream_auto_launch", "true")));
    }

    private void saveDefaultConfig() throws IOException {
        saveConfig(new Config());
    }

    private void saveConfig(Config config) throws IOException {
        Properties file = new Properties();
        try (FileOutputStream output = new FileOutputStream(CONFIG_FILENAME)) {
            file.setProperty("language", config.getLanguage());
            file.setProperty("save_game_directory", config.getSaveGameDirectory());
            file.setProperty("username", config.getUsername());
            file.setProperty("password", config.getPassword());
            file.setProperty("receive_filename", config.getReceiveFilename());
            file.setProperty("relay_server", config.getRelayServer());
            file.setProperty("upstream_notifications_enabled", String.valueOf(config.isUpstreamNotifications()));
            file.setProperty("downstream_overlay_enabled", String.valueOf(config.isDownstreamOverlay()));
            file.setProperty("downstream_auto_launch", String.valueOf(config.isAutoLaunch()));

            file.store(output, null);

            System.out.println(output.getFD().toString());
        }
    }


}
