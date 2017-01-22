package com.kevinpthorne.aoe2hdspectator.config.controller;

import com.kevinpthorne.aoe2hdspectator.config.model.Config;
import javafx.fxml.FXML;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;

import java.io.IOException;

/**
 * Created by kevint on 1/16/2017.
 */
public class Controller {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private TextField savegameDirectory;

    @FXML
    private TextField receiveFilenameField;

    @FXML
    private TextField relayServer;

    @FXML
    private CheckBox showDownstreamStatus;

    @FXML
    private CheckBox autoLaunch;

    @FXML
    private CheckBox silentUpstream;

    @FXML
    private Label statusText;

    private Config currentConfig;

    public Controller() {
    }

    public void setError(String message) {
        statusText.setText(message);
    }

    public void bind(Config current) {
        this.currentConfig = current;
        try {
            usernameField.setText(current.getUsername());
        } catch (NullPointerException e) {
        }
        try {
            passwordField.setText(current.getPassword());
        } catch (NullPointerException e) {
        }
        try {
            savegameDirectory.setText(current.getSaveGameDirectory());
        } catch (NullPointerException e) {
        }
        try {
            receiveFilenameField.setText(current.getReceiveFilename());
        } catch (NullPointerException e) {
        }
        try {
            relayServer.setText(current.getRelayServer());
        } catch (NullPointerException e) {
        }

        try {
            showDownstreamStatus.setSelected(current.isDownstreamOverlay());
        } catch (NullPointerException e) {
        }

        try {
            autoLaunch.setSelected(current.isAutoLaunch());
        } catch (NullPointerException e) {
        }

        try {
            silentUpstream.setSelected(!current.isUpstreamNotifications());
        } catch (NullPointerException e) {
        }

    }

    public void reset() {
        bind(new Config());
    }

    public void save() {
        if (!relayServer.getText().matches("[^\\:]+(:[0-9]{3,5})?")) {
            statusText.setText("Invalid Relay Server");
            return;
        }
        //TODO more validation

        try {
            currentConfig.setUsername(usernameField.getText());
        } catch (NullPointerException e) {
        }
        try {
            currentConfig.setPassword(passwordField.getText());
        } catch (NullPointerException e) {
        }
        try {
            currentConfig.setSaveGameDirectory(savegameDirectory.getText());
        } catch (NullPointerException e) {
        }
        try {
            currentConfig.setReceiveFilename(receiveFilenameField.getText());
        } catch (NullPointerException e) {
        }
        try {
            currentConfig.setRelayServer(relayServer.getText());
        } catch (NullPointerException e) {
        }

        try {
            currentConfig.setDownstreamOverlay(showDownstreamStatus.isSelected());
        } catch (NullPointerException e) {
        }

        try {
            currentConfig.setAutoLaunch(autoLaunch.isSelected());
        } catch (NullPointerException e) {
        }

        try {
            currentConfig.setUpstreamNotifications(!silentUpstream.isSelected());
        } catch (NullPointerException e) {
        }

        try {
            Config.save(currentConfig);
            close();
        } catch (IOException e) {
            e.printStackTrace();
            statusText.setText(e.getMessage());
        }


    }

    public void close() {
        System.exit(0);
    }

}
