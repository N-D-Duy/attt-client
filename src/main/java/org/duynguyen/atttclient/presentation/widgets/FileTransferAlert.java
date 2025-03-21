package org.duynguyen.atttclient.presentation.widgets;

import javafx.application.Platform;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Modality;
import org.duynguyen.atttclient.protocol.FileTransfer;
import org.duynguyen.atttclient.utils.Log;

import java.text.DecimalFormat;
import java.util.concurrent.atomic.AtomicBoolean;

public class FileTransferAlert {
    private static Alert alert;
    private static ProgressBar progressBar;
    private static Label statusLabel;
    private static Label progressLabel;
    private static FileTransfer currentTransfer;
    private static final AtomicBoolean isShowing = new AtomicBoolean(false);
    private static Thread updateThread;
    private static final DecimalFormat df = new DecimalFormat("0.0");

    public static void show(FileTransfer transfer) {
        if (isShowing.compareAndSet(false, true)) {
            currentTransfer = transfer;

            Platform.runLater(() -> {
                createAlert();
                startUpdateThread();

                alert.showAndWait().ifPresent(result -> {
                    if (result == ButtonType.CANCEL) {
                        // Handle cancel
                        Log.info("Transfer canceled by user");
                    }
                    isShowing.set(false);
                    if (updateThread != null) {
                        updateThread.interrupt();
                    }
                });
            });
        } else {
            currentTransfer = transfer;
        }
    }

    private static void createAlert() {
        alert = new Alert(Alert.AlertType.NONE);
        alert.setTitle("File Transfer");
        alert.setHeaderText("Transferring: " + currentTransfer.getFileName());
        alert.initModality(Modality.NONE); // Non-blocking

        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().add(cancelButtonType);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPrefWidth(400);

        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        GridPane.setHgrow(progressBar, Priority.ALWAYS);

        statusLabel = new Label("Initializing...");
        progressLabel = new Label("0%");

        grid.add(new Label("Status:"), 0, 0);
        grid.add(statusLabel, 1, 0);
        grid.add(progressBar, 0, 1, 2, 1);
        grid.add(progressLabel, 0, 2, 2, 1);

        alert.getDialogPane().setContent(grid);
    }

    private static void startUpdateThread() {
        if (updateThread != null) {
            updateThread.interrupt();
        }

        updateThread = new Thread(() -> {
            try {
                while (isShowing.get() && !Thread.interrupted()) {
                    updateProgress();
                    Thread.sleep(200);
                }
            } catch (InterruptedException e) {
                Log.info("Progress update thread interrupted");
            } catch (Exception e) {
                Log.error("Error updating progress: " + e.getMessage());
            }
        });

        updateThread.setDaemon(true);
        updateThread.start();
    }

    private static void updateProgress() {
        if (currentTransfer != null && isShowing.get()) {
            Platform.runLater(() -> {
                double progress = currentTransfer.getProgress();
                progressBar.setProgress(progress);
                progressLabel.setText(df.format(progress * 100) + "% - " +
                        formatFileSize(currentTransfer.getBytesTransferred()) + " / " +
                        formatFileSize(currentTransfer.getFileSize()));

                switch (currentTransfer.getState()) {
                    case ENCRYPTING -> statusLabel.setText("Encrypting file...");
                    case TRANSFERRING -> statusLabel.setText(currentTransfer.isSender() ? "Sending file..." : "Receiving file...");
                    case DECRYPTING -> statusLabel.setText("Decrypting file...");
                    case COMPLETED -> {
                        statusLabel.setText("Transfer completed!");
                        Platform.runLater(() -> alert.close());
                    }
                    case FAILED -> {
                        statusLabel.setText("Transfer failed!");
                        Platform.runLater(() -> alert.close());
                        /*new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                if (isShowing.get()) {
                                    Platform.runLater(() -> alert.close());
                                }
                            } catch (InterruptedException ignored) {}
                        }).start();*/
                    }
                    default -> statusLabel.setText("Processing...");
                }
            });
        }
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp-1);
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }
}