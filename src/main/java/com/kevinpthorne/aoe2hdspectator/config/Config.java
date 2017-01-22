package com.kevinpthorne.aoe2hdspectator.config;

/**
 * Created by kevint on 1/19/2017.
 */
public class Config {

    private String language;

    private String saveGameDirectory;

    private String username;
    private String password;

    private String receiveFilename;
    private String relayServer;

    private boolean upstreamNotifications;

    private boolean downstreamOverlay;
    private boolean autoLaunch;

    public Config() {
        this("en", "", "", "", "game-spectating", "live.aoe2.net", false, true, true);
    }

    public Config(String language, String saveGameDir, String username, String password, String receiveFilename, String relayServer,
                  boolean upstreamNotifications, boolean downstreamOverlay, boolean autoLaunch) {

        this.language = language;

        this.saveGameDirectory = saveGameDir;

        this.username = username;
        this.password = password;

        this.receiveFilename = receiveFilename;
        this.relayServer = relayServer;

        this.upstreamNotifications = upstreamNotifications;
        this.downstreamOverlay = downstreamOverlay;
        this.autoLaunch = autoLaunch;

    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSaveGameDirectory() {
        return saveGameDirectory;
    }

    public void setSaveGameDirectory(String savegameDirectory) {
        this.saveGameDirectory = savegameDirectory;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getReceiveFilename() {
        return receiveFilename;
    }

    public void setReceiveFilename(String receiveFilename) {
        this.receiveFilename = receiveFilename;
    }

    public String getRelayServer() {
        return relayServer;
    }

    public void setRelayServer(String relayServer) {
        this.relayServer = relayServer;
    }

    public boolean isUpstreamNotifications() {
        return upstreamNotifications;
    }

    public void setUpstreamNotifications(boolean upstreamNotifications) {
        this.upstreamNotifications = upstreamNotifications;
    }

    public boolean isDownstreamOverlay() {
        return downstreamOverlay;
    }

    public void setDownstreamOverlay(boolean downstreamOverlay) {
        this.downstreamOverlay = downstreamOverlay;
    }

    public boolean isAutoLaunch() {
        return autoLaunch;
    }

    public void setAutoLaunch(boolean autoLaunch) {
        this.autoLaunch = autoLaunch;
    }
}
