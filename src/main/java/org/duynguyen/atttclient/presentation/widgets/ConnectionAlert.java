package org.duynguyen.atttclient.presentation.widgets;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ConnectionAlert {
    private static final AtomicBoolean isShowing = new AtomicBoolean(false);
    private static Alert alert;
    private static ProgressIndicator progressIndicator;
    private static Label statusLabel;
    private static Timer countdownTimer;

    public static void showRetrying(int attempt, int maxAttempts, int delay) {
        stopCountdown();
        startCountdown(delay, (remainingSeconds) -> {
            updateStatus("Connection failed. Retrying in " + remainingSeconds + " seconds... (Attempt " + attempt + "/" + maxAttempts + ")", true);
        });
    }

    public static void showSuccess() {
        stopCountdown();
        updateStatus("Connected successfully!", false);
        closeAfterDelay(1000);
    }

    public static void showFailed(Runnable retryAction) {
        stopCountdown();
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

    private static void createAlert(String headerText) {
        Platform.runLater(() -> {
            alert = new Alert(Alert.AlertType.NONE);
            alert.setTitle("Connecting to Server");
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

            alert.setOnCloseRequest(event -> {
                isShowing.set(false);
                stopCountdown();
            });
            alert.show();
        });
    }

    public static void showConnecting(String host, int port) {
        stopCountdown();
        if (isShowing.compareAndSet(false, true)) {
            createAlert("Attempting to connect to " + host + ":" + port);
            updateStatus("Connecting...", true);
        } else {
            updateStatus("Connecting...", true);
        }
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
            } catch (InterruptedException ignored) {
            }
        }).start();
    }

    private static void startCountdown(int totalSeconds, CountdownCallback callback) {
        stopCountdown();

        countdownTimer = new Timer(true);
        AtomicInteger secondsRemaining = new AtomicInteger(totalSeconds);

        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                int remaining = secondsRemaining.decrementAndGet();
                callback.onTick(remaining);

                if (remaining <= 0) {
                    stopCountdown();
                }
            }
        }, 0, 1000);
    }

    private static void stopCountdown() {
        if (countdownTimer != null) {
            countdownTimer.cancel();
            countdownTimer = null;
        }
    }

    private interface CountdownCallback {
        void onTick(int secondsRemaining);
    }
}