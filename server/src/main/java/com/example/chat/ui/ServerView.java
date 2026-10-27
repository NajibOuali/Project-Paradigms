package com.example.chat.ui;

import com.example.chat.server.ServerEventListener;
import com.example.chat.server.TCPServerModel;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.io.IOException;
import java.util.List;

public class ServerView implements ServerEventListener {
    private final TCPServerModel serverModel;
    private final GridPane mainGrid;
    private final TextArea logArea;
    private final ListView<String> clientListView;
    private final ObservableList<String> activeUsersList;
    private final Circle statusCircle;
    private final Label statusLabel;
    private final Button startStopButton;

    public ServerView(TCPServerModel serverModel) {
        this.serverModel = serverModel;
        this.serverModel.addListener(this);

        this.mainGrid = new GridPane();
        this.logArea = new TextArea();
        this.activeUsersList = FXCollections.observableArrayList();
        this.clientListView = new ListView<>(activeUsersList);
        this.statusCircle = new Circle(6, Color.web("#dc2626"));
        this.statusLabel = new Label("Stopped");
        this.startStopButton = new Button("Start Server");

        buildUI();
    }

    private void buildUI() {
        mainGrid.setPadding(new Insets(15));
        mainGrid.setHgap(15);
        mainGrid.setVgap(15);

        // Header
        VBox headerBox = new VBox(4);
        Label titleLabel = new Label("TCP Chat Server");
        titleLabel.getStyleClass().add("title-label");
        Label subtitleLabel = new Label("Host: " + serverModel.getHost() + " | Port: " + serverModel.getPort());
        subtitleLabel.getStyleClass().add("subtitle-label");
        headerBox.getChildren().addAll(titleLabel, subtitleLabel);

        // Status bar & control button
        HBox statusBox = new HBox(10);
        statusBox.setAlignment(Pos.CENTER_RIGHT);
        statusLabel.getStyleClass().add("status-label");
        statusLabel.getStyleClass().add("status-offline");

        startStopButton.getStyleClass().add("button-primary");
        startStopButton.setOnAction(e -> toggleServer());

        HBox indicator = new HBox(6, statusCircle, statusLabel);
        indicator.setAlignment(Pos.CENTER_LEFT);
        statusBox.getChildren().addAll(indicator, startStopButton);

        // Top Row: Header (Left) and Controls (Right)
        GridPane topBar = new GridPane();
        topBar.setHgap(10);
        ColumnConstraints c1 = new ColumnConstraints();
        c1.setHgrow(Priority.ALWAYS);
        ColumnConstraints c2 = new ColumnConstraints();
        c2.setHgrow(Priority.NEVER);
        topBar.getColumnConstraints().addAll(c1, c2);
        topBar.add(headerBox, 0, 0);
        topBar.add(statusBox, 1, 0);

        // Log section
        VBox logBox = new VBox(8);
        Label logTitle = new Label("Activity Logs");
        logTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #334155;");
        logArea.setEditable(false);
        logArea.setWrapText(true);
        VBox.setVgrow(logArea, Priority.ALWAYS);
        logBox.getChildren().addAll(logTitle, logArea);

        // Client List section
        VBox clientsBox = new VBox(8);
        Label clientTitle = new Label("Connected Clients");
        clientTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px; -fx-text-fill: #334155;");
        clientListView.setCellFactory(param -> new ColoredUserCell());
        clientListView.setPlaceholder(new Label("No clients connected"));
        VBox.setVgrow(clientListView, Priority.ALWAYS);
        clientsBox.getChildren().addAll(clientTitle, clientListView);

        // Grid setup
        // Row 0: Top bar
        // Row 1: Content (Logs + Clients)
        ColumnConstraints colLogs = new ColumnConstraints();
        colLogs.setPercentWidth(65);
        colLogs.setHgrow(Priority.ALWAYS);

        ColumnConstraints colClients = new ColumnConstraints();
        colClients.setPercentWidth(35);
        colClients.setHgrow(Priority.ALWAYS);

        mainGrid.getColumnConstraints().addAll(colLogs, colClients);

        RowConstraints rowTop = new RowConstraints();
        rowTop.setVgrow(Priority.NEVER);
        RowConstraints rowMain = new RowConstraints();
        rowMain.setVgrow(Priority.ALWAYS);

        mainGrid.getRowConstraints().addAll(rowTop, rowMain);

        mainGrid.add(topBar, 0, 0, 2, 1);
        mainGrid.add(logBox, 0, 1);
        mainGrid.add(clientsBox, 1, 1);
    }

    public Parent getView() {
        return mainGrid;
    }

    private void toggleServer() {
        if (serverModel.isRunning()) {
            serverModel.stop();
            updateServerStatus(false);
        } else {
            try {
                serverModel.start();
                updateServerStatus(true);
            } catch (IOException ex) {
                onLog("[ERROR] Failed to start server: " + ex.getMessage());
            }
        }
    }

    public void updateServerStatus(boolean isRunning) {
        Platform.runLater(() -> {
            if (isRunning) {
                statusCircle.setFill(Color.web("#22c55e"));
                statusLabel.setText("Running");
                statusLabel.getStyleClass().removeAll("status-offline");
                statusLabel.getStyleClass().add("status-online");
                startStopButton.setText("Stop Server");
                startStopButton.getStyleClass().removeAll("button-primary");
                startStopButton.getStyleClass().add("button-danger");
            } else {
                statusCircle.setFill(Color.web("#dc2626"));
                statusLabel.setText("Stopped");
                statusLabel.getStyleClass().removeAll("status-online");
                statusLabel.getStyleClass().add("status-offline");
                startStopButton.setText("Start Server");
                startStopButton.getStyleClass().removeAll("button-danger");
                startStopButton.getStyleClass().add("button-primary");
                activeUsersList.clear();
            }
        });
    }

    @Override
    public void onLog(String message) {
        Platform.runLater(() -> {
            logArea.appendText(message + "\n");
        });
    }

    @Override
    public void onUserJoined(String username) {
        // UI is updated via onUserListUpdated
    }

    @Override
    public void onUserLeft(String username) {
        // UI is updated via onUserListUpdated
    }

    @Override
    public void onUserListUpdated(List<String> activeUsers) {
        Platform.runLater(() -> {
            activeUsersList.setAll(activeUsers);
        });
    }
}
