package com.kevinpthorne.aoe2hdspectator.config;

import com.kevinpthorne.aoe2hdspectator.config.controller.Controller;
import com.kevinpthorne.aoe2hdspectator.config.model.Config;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;

/**
 * Created by kevint on 1/16/2017.
 */
public class MainApp extends Application {

    private Stage primaryStage;

    public static void main(String[] args) {
        launch(args);
    }

    public void start(Stage primaryStage) throws Exception {
        this.primaryStage = primaryStage;
        this.primaryStage.setTitle("Age of Empires 2 HD - Spectator Settings");

        loadContent("main");
    }

//    private void initRootLayout() {
//        try {
//            // Load root layout from fxml file.
//            FXMLLoader loader = new FXMLLoader();
//            loader.setLocation(MainApp.class.getResource("view/RootLayout.fxml"));
//            rootLayout = loader.bind();
//
//            // Show the scene containing the root layout.
//            Scene scene = new Scene(rootLayout);
//            primaryStage.setScene(scene);
//            primaryStage.show();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    private void loadContent(String layout) {
        try {
            // Load person overview.
            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(MainApp.class.getResource("/views/" + layout + ".fxml"));
            VBox rootLayout = loader.load();

            Scene scene = new Scene(rootLayout);
            primaryStage.setScene(scene);
            primaryStage.show();

            Controller main = loader.getController();
            try {
                main.bind(Config.parse());
            } catch (IOException e) {
                main.bind(new Config());
                main.setError(e.getMessage());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }
}
