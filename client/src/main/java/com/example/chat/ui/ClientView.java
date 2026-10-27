package com.example.chat.ui;

import com.example.chat.client.ClientEventListener;
import com.example.chat.client.TCPClientModel;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

public class ClientView implements ClientEventListener {
    private final TCPClientModel clientModel;
    private final GridPane mainGrid;
    private final TextArea chatArea;
    private final TextField messageField;
    private final Button sendButton;
    private final Button allUsersButton;
    private final Button leaveButton;
    private final Circle statusCircle;
    private final Label statusLabel;
    private final Label userModeLabel;

    public ClientView(TCPClientModel clientModel) {
        this.clientModel = clientModel;
        this.clientModel.addListener(this);

        this.mainGrid = new GridPane();
        this.chatArea = new TextArea();
        this.messageField = new TextField();
        this.sendButton = new Button("SEND");
        this.allUsersButton = new Button("allUsers");
        this.leaveButton = new Button("LEAVE");
        this.statusCircle = new Circle(6, Color.web("#dc2626"));
        this.statusLabel = new Label("Offline");
        this.userModeLabel = new Label();

        buildUI();
    }

    private void buildUI() {
        mainGrid.setPadding(new Insets(15));
        mainGrid.setHgap(12);
        mainGrid.setVgap(12);

        // Header
        VBox headerBox = new VBox(4);
        Label titleLabel = new Label("Group Chat Client");
        titleLabel.getStyleClass().add("title-label");

        String displayUser = clientModel.isReadOnly() ? "Guest (Read-Only)" : clientModel.getUsername();
        Label subtitleLabel = new Label("Logged in as: " + displayUser + " | Server: " + clientModel.getHost() + ":" + clientModel.getPort());
        subtitleLabel.getStyleClass().add("subtitle-label");
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Status indicator
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_RIGHT);

        HBox indicator = new HBox(6, statusCircle, statusLabel);
        indicator.setAlignment(Pos.CENTER_LEFT);

        leaveButton.getStyleClass().add("button-danger");
        leaveButton.setOnAction(e -> handleLeave());

        allUsersButton.getStyleClass().add("button-secondary");
        allUsersButton.setOnAction(e -> clientModel.requestActiveUsers());

        statusBox.getChildren().addAll(indicator, allUsersButton, leaveButton);

        // Top Row GridPane
        GridPane topBar = new GridPane();
        topBar.setHgap(10);
        ColumnConstraints cTop1 = new ColumnConstraints();
        cTop1.setHgrow(Priority.ALWAYS);
        ColumnConstraints cTop2 = new ColumnConstraints();
        cTop2.setHgrow(Priority.NEVER);
        topBar.getColumnConstraints().addAll(cTop1, cTop2);
        topBar.add(headerBox, 0, 0);
        topBar.add(statusBox, 1, 0);

        // Read-only notification banner if applicable
        if (clientModel.isReadOnly()) {
            userModeLabel.setText("⚠️ READ-ONLY MODE: You did not provide a username. You cannot send messages.");
            userModeLabel.setStyle("-fx-background-color: #fef3c7; -fx-text-fill: #92400e; -fx-padding: 8px 12px; -fx-background-radius: 6px; -fx-font-weight: bold;");
            messageField.setDisable(true);
            messageField.setPromptText("Messaging disabled in Read-Only Mode");
            sendButton.setDisable(true);
        } else {
            messageField.setPromptText("Type a message or command ('allUsers', 'bye', 'end') and press Enter...");
        }

        // Chat display area
        chatArea.setEditable(false);
        chatArea.setWrapText(true);
        VBox.setVgrow(chatArea, Priority.ALWAYS);

        // Message input area
        HBox inputBar = new HBox(10);
        HBox.setHgrow(messageField, Priority.ALWAYS);

        sendButton.getStyleClass().add("button-primary");
        sendButton.setOnAction(e -> handleSendMessage());

        messageField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleSendMessage();
            }
        });

        inputBar.getChildren().addAll(messageField, sendButton);

        // Main grid setup
        ColumnConstraints colMain = new ColumnConstraints();
        colMain.setHgrow(Priority.ALWAYS);
        mainGrid.getColumnConstraints().add(colMain);

        RowConstraints r0 = new RowConstraints();
        r0.setVgrow(Priority.NEVER);
        RowConstraints rBanner = new RowConstraints();
        rBanner.setVgrow(Priority.NEVER);
        RowConstraints rChat = new RowConstraints();
        rChat.setVgrow(Priority.ALWAYS);
        RowConstraints rInput = new RowConstraints();
        rInput.setVgrow(Priority.NEVER);

        mainGrid.getRowConstraints().addAll(r0, rBanner, rChat, rInput);

        mainGrid.add(topBar, 0, 0);
        if (clientModel.isReadOnly()) {
            mainGrid.add(userModeLabel, 0, 1);
        }
        mainGrid.add(chatArea, 0, 2);
        mainGrid.add(inputBar, 0, 3);
    }

    public Parent getView() {
        return mainGrid;
    }

    private void handleSendMessage() {
        String text = messageField.getText();
        if (text != null && !text.trim().isEmpty()) {
            clientModel.sendMessage(text.trim());
            messageField.clear();
        }
    }

    private void handleLeave() {
        clientModel.disconnect("Left chat room");
    }

    @Override
    public void onMessageReceived(String message) {
        Platform.runLater(() -> {
            chatArea.appendText(message + "\n");
        });
    }

    @Override
    public void onConnected() {
        Platform.runLater(() -> {
            statusCircle.setFill(Color.web("#22c55e"));
            statusLabel.setText("Online");
            statusLabel.getStyleClass().removeAll("status-offline");
            statusLabel.getStyleClass().add("status-online");
            leaveButton.setDisable(false);
            if (!clientModel.isReadOnly()) {
                messageField.setDisable(false);
                sendButton.setDisable(false);
            }
        });
    }

    @Override
    public void onDisconnected(String reason) {
        Platform.runLater(() -> {
            statusCircle.setFill(Color.web("#dc2626"));
            statusLabel.setText("Offline");
            statusLabel.getStyleClass().removeAll("status-online");
            statusLabel.getStyleClass().add("status-offline");
            chatArea.appendText("[SYSTEM] " + reason + "\n");
            messageField.setDisable(true);
            sendButton.setDisable(true);
            leaveButton.setDisable(true);
        });
    }

    @Override
    public void onError(String errorMessage) {
        Platform.runLater(() -> {
            chatArea.appendText("[SYSTEM ERROR] " + errorMessage + "\n");
        });
    }
}
