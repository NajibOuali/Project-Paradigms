package com.example.chat;

import com.example.chat.client.TCPClientModel;
import com.example.chat.config.NetworkConfig;
import com.example.chat.ui.ClientView;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Optional;

public class TCPClient extends Application {
    private static String cmdServerIp = null;
    private static Integer cmdServerPort = null;
    private TCPClientModel clientModel;

    @Override
    public void start(Stage primaryStage) {
        NetworkConfig config = new NetworkConfig();
        String host = (cmdServerIp != null && !cmdServerIp.isBlank()) ? cmdServerIp : config.getServerIp();
        int port = (cmdServerPort != null && cmdServerPort > 0) ? cmdServerPort : config.getServerPort();

        // Prompt user for username or Read-Only mode
        String username = promptUsernameDialog(host, port);

        clientModel = new TCPClientModel(host, port, username);
        ClientView clientView = new ClientView(clientModel);

        Scene scene = new Scene(clientView.getView(), 650, 500);
        String css = getClass().getResource("/styles.css") != null ? getClass().getResource("/styles.css").toExternalForm() : null;
        if (css != null) {
            scene.getStylesheets().add(css);
        }

        String userDisplay = clientModel.isReadOnly() ? "Guest (Read-Only)" : username;
        primaryStage.setTitle("TCP Group Chat Client - [" + userDisplay + "]");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> {
            if (clientModel != null) {
                clientModel.disconnect("Application closed");
            }
            System.exit(0);
        });

        primaryStage.show();

        // Connect to server
        try {
            clientModel.connect();
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Connection Error");
            alert.setHeaderText("Failed to connect to server");
            alert.setContentText("Could not connect to " + host + ":" + port + "\nError: " + e.getMessage());
            alert.showAndWait();
        }
    }

    private String promptUsernameDialog(String host, int port) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Chat Login");
        dialog.setHeaderText("Connect to Server: " + host + ":" + port);

        ButtonType loginButtonType = new ButtonType("Join Chat", ButtonBar.ButtonData.OK_DONE);
        ButtonType readOnlyButtonType = new ButtonType("Read-Only Mode", ButtonBar.ButtonData.OTHER);
        dialog.getDialogPane().getButtonTypes().addAll(loginButtonType, readOnlyButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");

        grid.add(new Label("Username:"), 0, 0);
        grid.add(usernameField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        Platform.runLater(usernameField::requestFocus);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == loginButtonType) {
                return usernameField.getText().trim();
            } else if (dialogButton == readOnlyButtonType) {
                return ""; // Empty signifies Read-Only Mode
            } else if (dialogButton == ButtonType.CANCEL) {
                System.exit(0);
            }
            return "";
        });

        Optional<String> result = dialog.showAndWait();
        return result.orElse("");
    }

    @Override
    public void stop() {
        if (clientModel != null) {
            clientModel.disconnect("Application stopped");
        }
    }

    public static void main(String[] args) {
        // Handle CLI arguments: java TCPClient <ServerIPAddress> <PortNumber>
        if (args.length >= 1) {
            cmdServerIp = args[0];
        }
        if (args.length >= 2) {
            try {
                cmdServerPort = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[1] + ". Using config default.");
            }
        }
        launch(args);
    }
}
