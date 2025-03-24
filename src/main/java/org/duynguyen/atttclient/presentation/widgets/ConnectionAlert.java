package org.duynguyen.atttclient.presentation.widgets;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.util.concurrent.atomic.AtomicBoolean;

public class ConnectionAlert {
    private static Alert alert;
    private static ProgressIndicator progressIndicator;
    private static Label statusLabel;
    private static final AtomicBoolean isShowing = new AtomicBoolean(false);

    public static void showConnecting(String host, int port) {
        if (isShowing.compareAndSet(false, true)) {
            createAlert("Connecting to Server", "Attempting to connect to " + host + ":" + port);
            updateStatus("Connecting...", true);
            alert.show();
        } else {
            updateStatus("Connecting...", true);
        }
    }

    public static void showRetrying(String host, int port, int attempt, int maxAttempts, int delay) {
        updateStatus("Connection failed. Retrying in " + delay + " seconds... (Attempt " + attempt + "/" + maxAttempts + ")", true);
    }

    public static void showSuccess() {
        updateStatus("Connected successfully!", false);
        closeAfterDelay(1500);
    }

    public static void showFailed(String host, int port, Runnable retryAction) {
        Platform.runLater(() -> {
            statusLabel.setText("Failed to connect after multiple attempts");
            progressIndicator.setVisible(false);

            Button retryButton = new Button("Retry Connection");
            retryButton.setOnAction(e -> {
                alert.close();
                isShowing.set(false);
                retryAction.run();
            });

            HBox buttonBox = new HBox(10, retryButton);
            buttonBox.setAlignment(Pos.CENTER);

            VBox content = (VBox) alert.getDialogPane().getContent();
            content.getChildren().add(buttonBox);
        });
    }

    private static void createAlert(String title, String headerText) {
        Platform.runLater(() -> {
            alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle(title);
            alert.setHeaderText(headerText);
            alert.initModality(Modality.APPLICATION_MODAL);

            progressIndicator = new ProgressIndicator();
            progressIndicator.setPrefSize(30, 30);

            statusLabel = new Label("Initializing connection...");

            VBox content = new VBox(15);
            content.setAlignment(Pos.CENTER);
            content.getChildren().addAll(progressIndicator, statusLabel);
            content.setPrefWidth(350);
            content.setPrefHeight(120);

            alert.getDialogPane().setContent(content);

            
            ButtonType closeButton = new ButtonType("Close", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().add(closeButton);
            Button cancelButton = (Button) alert.getDialogPane().lookupButton(closeButton);
            cancelButton.setVisible(false);

            alert.setOnCloseRequest(event -> isShowing.set(false));
        });
    }

    private static void updateStatus(String status, boolean showProgress) {
        Platform.runLater(() -> {
            if (statusLabel != null) {
                statusLabel.setText(status);
            }
            if (progressIndicator != null) {
                progressIndicator.setVisible(showProgress);
            }
        });
    }

    private static void closeAfterDelay(long delayMs) {
        new Thread(() -> {
            try {
                Thread.sleep(delayMs);
                Platform.runLater(() -> {
                    if (alert != null && isShowing.get()) {
                        alert.close();
                        isShowing.set(false);
                    }
                });
            } catch (InterruptedException ignored) {}
        }).start();
    }
}