package com.example.chat;

import com.example.chat.config.NetworkConfig;
import com.example.chat.server.TCPServerModel;
import com.example.chat.ui.ServerView;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;

public class TCPServer extends Application {
    private TCPServerModel serverModel;

    @Override
    public void start(Stage primaryStage) {
        NetworkConfig config = new NetworkConfig();
        serverModel = new TCPServerModel(config.getServerIp(), config.getServerPort());
        ServerView serverView = new ServerView(serverModel);

        Scene scene = new Scene(serverView.getView(), 700, 500);
        String css = getClass().getResource("/styles.css") != null ? getClass().getResource("/styles.css").toExternalForm() : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        primaryStage.setTitle("TCP Chat Server - Group Chat");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            if (serverModel != null) {
                serverModel.stop();
            }
            System.exit(0);
        });

        primaryStage.show();

        // Auto-start the server upon launch
        try {
            serverModel.start();
            serverView.updateServerStatus(true);
        } catch (IOException e) {
            System.err.println("Could not auto-start server: " + e.getMessage());
        }
    }

    @Override
    public void stop() {
        if (serverModel != null) {
            serverModel.stop();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
