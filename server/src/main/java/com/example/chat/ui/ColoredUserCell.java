package com.example.chat.ui;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ColoredUserCell extends ListCell<String> {
    private static final Map<String, String> USER_COLOR_MAP = new ConcurrentHashMap<>();
    private static final String[] PRESET_COLORS = {
            "#fee2e2", "#fef3c7", "#dcfce7", "#e0e7ff", "#f3e8ff",
            "#ffe4e6", "#dbeafe", "#ffedd5", "#ecfdf5", "#f1f5f9"
    };
    private static final String[] TEXT_COLORS = {
            "#991b1b", "#92400e", "#166534", "#3730a3", "#6b21a8",
            "#9f1239", "#1e40af", "#9a3412", "#065f46", "#334155"
    };

    private final HBox container = new HBox(8);
    private final Circle statusCircle = new Circle(4, Color.web("#22c55e"));
    private final Label nameLabel = new Label();

    public ColoredUserCell() {
        container.setAlignment(Pos.CENTER_LEFT);
        container.getChildren().addAll(statusCircle, nameLabel);
    }

    private static String getBackgroundColorForUser(String username) {
        return USER_COLOR_MAP.computeIfAbsent(username, k -> {
            int hash = Math.abs(k.hashCode());
            return PRESET_COLORS[hash % PRESET_COLORS.length];
        });
    }

    private static String getTextColorForUser(String username) {
        int hash = Math.abs(username.hashCode());
        return TEXT_COLORS[hash % TEXT_COLORS.length];
    }

    @Override
    protected void updateItem(String username, boolean empty) {
        super.updateItem(username, empty);

        if (empty || username == null || username.isBlank()) {
            setText(null);
            setGraphic(null);
            setStyle("-fx-background-color: transparent;");
        } else {
            nameLabel.setText(username);
            String bg = getBackgroundColorForUser(username);
            String textColor = getTextColorForUser(username);
            nameLabel.setStyle("-fx-text-fill: " + textColor + "; -fx-font-weight: bold;");

            setStyle("-fx-background-color: " + bg + "; -fx-background-radius: 4px; -fx-padding: 4px 8px; -fx-margin: 2px;");
            setGraphic(container);
        }
    }
}
