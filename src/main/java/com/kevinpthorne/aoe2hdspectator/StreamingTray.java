package com.kevinpthorne.aoe2hdspectator;

import com.kevinpthorne.aoe2hdspectator.config.Config;
import com.kevinpthorne.aoe2hdspectator.io.Language;

import javax.swing.*;
import java.awt.AWTException;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.logging.Logger;

/**
 * Created by kevint on 1/19/2017.
 */
class StreamingTray implements ActionListener {

    private TrayListener listener;
    private Config config;
    private Language language;

    private TrayIcon trayIcon;

    private Logger log = StreamingApp.log;

    StreamingTray(TrayListener listener, Config config, Language language) throws AWTException {
        this.listener = listener;
        this.config = config;
        this.language = language;

        if (!SystemTray.isSupported()) {
            log.severe("SystemTray is not supported");
            throw new RuntimeException("Could not initialize tray application, its not supported");
        }

        final PopupMenu menu = new PopupMenu();

        trayIcon = new TrayIcon(new ImageIcon(StreamingApp.status.getIconResource()).getImage());

        buildMenu(menu);

        trayIcon.setPopupMenu(menu);
        trayIcon.setImageAutoSize(true);

        SystemTray.getSystemTray().add(trayIcon);

        updateStatus();
    }

    private void buildMenu(PopupMenu menu) {
        MenuItem upstreamItem = new MenuItem(language.getString("action_upstream"));
        MenuItem downstreamItem = new MenuItem(language.getString("action_downstream"));
        MenuItem settingsItem = new MenuItem(language.getString("action_settings"));
        MenuItem exitItem = new MenuItem(language.getString("action_exit"));

        upstreamItem.setActionCommand("up");
        downstreamItem.setActionCommand("down");
        settingsItem.setActionCommand("config");
        exitItem.setActionCommand("exit");

        menu.add(upstreamItem);
        menu.add(downstreamItem);
        menu.addSeparator();
        menu.add(settingsItem);
        menu.add(exitItem);

        upstreamItem.addActionListener(this);
        downstreamItem.addActionListener(this);
        settingsItem.addActionListener(this);
        exitItem.addActionListener(this);
    }

    public void updateStatus() {
        trayIcon.setImage(new ImageIcon(StreamingApp.status.getIconResource()).getImage());
        trayIcon.setToolTip(language.getString("status_" + StreamingApp.status.toString().toLowerCase()));
    }

    public void pushNotification(String title, String body) {
        trayIcon.displayMessage(title, body, TrayIcon.MessageType.NONE);
    }

    public void pushNotification(String title, String body, TrayIcon.MessageType type) {
        trayIcon.displayMessage(title, body, type);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        MenuItem item = (MenuItem) e.getSource();
        switch(e.getActionCommand()) {
            case "up":
                listener.onUpStream();
                break;
            case "down":
                listener.onDownStream();
                break;
            case "config":
                listener.onConfig();
                break;
            case "exit":
                listener.onExit();
                break;
        }
    }

    interface TrayListener {

        void onUpStream();

        void onDownStream();

        void onConfig();

        void onExit();

    }
}
