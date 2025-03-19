package org.duynguyen.atttclient.presentation.widgets;


import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.Setter;
import org.duynguyen.atttclient.protocol.FileTransfer;
import org.duynguyen.atttclient.utils.Log;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class FileTransferDialog {
    @Setter
    private static VBox dialogContainer;
    private static FileTransferDialog instance;
    private final VBox dialog;
    private final ProgressBar progressBar;
    private final Label statusLabel;
    private final Label fileInfoLabel;
    private final Label progressLabel;
    private FileTransfer currentTransfer;
    private final AtomicBoolean isShowing = new AtomicBoolean(false);
    private final AtomicBoolean shouldUpdate = new AtomicBoolean(false);
    private Consumer<FileTransfer> onCancelCallback;
    private Consumer<FileTransfer> onCompleteCallback;
    
    private FileTransferDialog() {
        dialog = new VBox(10);
        dialog.getStyleClass().add("file-transfer-dialog");
        dialog.setAlignment(Pos.CENTER);
        dialog.setMaxWidth(400);
        dialog.setPrefWidth(400);
        
        fileInfoLabel = new Label();
        fileInfoLabel.getStyleClass().add("file-info-label");
        
        progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(Double.MAX_VALUE);
        
        progressLabel = new Label("0%");
        
        statusLabel = new Label();
        statusLabel.getStyleClass().add("status-label");
        
        HBox progressBox = new HBox(10);
        progressBox.setAlignment(Pos.CENTER);
        HBox.setHgrow(progressBar, Priority.ALWAYS);
        progressBox.getChildren().addAll(progressBar, progressLabel);
        
        Button cancelButton = new Button("Cancel");
        cancelButton.getStyleClass().add("cancel-button");
        cancelButton.setOnAction(e -> {
            if (onCancelCallback != null && currentTransfer != null) {
                onCancelCallback.accept(currentTransfer);
            }
            hide();
        });
        
        dialog.getChildren().addAll(fileInfoLabel, progressBox, statusLabel, cancelButton);

        Thread updateThread = new Thread(this::updateProgress);
        updateThread.setDaemon(true);
        updateThread.start();
    }
    
    public static FileTransferDialog getInstance() {
        if (instance == null) {
            instance = new FileTransferDialog();
        }
        return instance;
    }

    public FileTransferDialog show(FileTransfer fileTransfer) {
        if (dialogContainer == null) {
            Log.error("Dialog container not set. Call FileTransferDialog.setDialogContainer first.");
            return this;
        }
        
        currentTransfer = fileTransfer;
        updateUI();
        
        if (!isShowing.getAndSet(true)) {
            Platform.runLater(() -> {
                if (!dialogContainer.getChildren().contains(dialog)) {
                    dialogContainer.getChildren().add(dialog);
                }
            });
        }
        
        shouldUpdate.set(true);
        return this;
    }
    
    public void hide() {
        shouldUpdate.set(false);
        if (isShowing.getAndSet(false)) {
            Platform.runLater(() -> dialogContainer.getChildren().remove(dialog));
        }
    }
    
    public FileTransferDialog onCancel(Consumer<FileTransfer> callback) {
        this.onCancelCallback = callback;
        return this;
    }
    
    public FileTransferDialog onComplete(Consumer<FileTransfer> callback) {
        this.onCompleteCallback = callback;
        return this;
    }
    
    private void updateUI() {
        if (currentTransfer == null) {
            return;
        }
        
        Platform.runLater(() -> {
            String fileName = currentTransfer.getFileName();
            long fileSize = currentTransfer.getFileSize();
            double progress = currentTransfer.getProgress();
            FileTransfer.FileTransferState state = currentTransfer.getState();
            
            fileInfoLabel.setText(fileName + " (" + formatFileSize(fileSize) + ")");
            progressBar.setProgress(progress);
            progressLabel.setText(String.format("%.0f%%", progress * 100));
            
            switch (state) {
                case INITIALIZED -> statusLabel.setText("Initializing...");
                case ENCRYPTING -> statusLabel.setText("Encrypting file...");
                case TRANSFERRING -> statusLabel.setText(currentTransfer.isSender() ? "Sending file..." : "Receiving file...");
                case COMPLETED -> {
                    statusLabel.setText("Transfer completed!");
                    if (onCompleteCallback != null) {
                        onCompleteCallback.accept(currentTransfer);
                    }
                }
                case DECRYPTING -> statusLabel.setText("Decrypting file...");
                case FAILED -> statusLabel.setText("Transfer failed!");
            }
        });
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }
    
    private void updateProgress() {
        while (true) {
            try {
                Thread.sleep(200);
                if (shouldUpdate.get() && currentTransfer != null) {
                    updateUI();
                    FileTransfer.FileTransferState state = currentTransfer.getState();
                    if (state == FileTransfer.FileTransferState.COMPLETED || 
                        state == FileTransfer.FileTransferState.FAILED) {
                        Thread.sleep(2000);
                        hide();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                Log.error("Error updating transfer progress: " + e.getMessage());
            }
        }
    }
}
