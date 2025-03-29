package org.duynguyen.atttclient.presentation;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.protocol.FileTransfer;
import org.duynguyen.atttclient.utils.Log;

import java.io.File;

public class FileSelectionController implements FileTransfer.TransferCompleteListener, FileTransfer.ProgressListener {
    private final Session session = Session.getInstance();
    private final FileTransfer fileTransfer = FileTransfer.instance;
    @FXML
    private Label fileInfoLabel;
    @FXML
    private ProgressBar progressBar;
    @FXML
    private Label progressLabel;
    @FXML
    private Label timeRemainingLabel;
    @FXML
    private Button btnEncryptFile;

    @FXML
    private Button btnSendFile;

    private File selectedFile;
    private File encryptedFile;

    @Setter
    private MainController mainController;

    @FXML
    public void initialize() {
        btnEncryptFile.setDisable(true);
        btnSendFile.setDisable(true);


        progressBar.setVisible(false);
        progressLabel.setVisible(false);
        timeRemainingLabel.setVisible(false);

        if (fileTransfer != null) {
            fileTransfer.setTransferCompleteListener(this);
            fileTransfer.setProgressListener(this);
        }
    }

    @Override
    public void onProgressUpdate(double progress, String estimatedTime) {
        progressBar.setProgress(progress);
        progressLabel.setText(String.format("%.1f%%", progress * 100));
        timeRemainingLabel.setText("Thời gian còn lại: " + estimatedTime);
    }

    @FXML
    private void onSendFile() {
        if (encryptedFile != null) {
            session.getService().sendFileInfo(fileTransfer.getTransferId(), selectedFile.getName(), encryptedFile.length());
            btnSendFile.setDisable(true);
            fileInfoLabel.setText("Đang gửi file...");
        }
    }

    @Override
    public void onTransferComplete() {
        Platform.runLater(() -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            timeRemainingLabel.setVisible(false);
            fileInfoLabel.setText("Gửi file thành công!");
            Stage stage = (Stage) fileInfoLabel.getScene().getWindow();
            stage.close();

            if (mainController != null) {
                mainController.showMainScreen();
            }

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Thông báo");
            alert.setHeaderText(null);
            alert.setContentText("Gửi file thành công!");
            alert.showAndWait();
        });
    }

    @Override
    public void onTransferFailed(String reason) {
        Platform.runLater(() -> {
            progressBar.setVisible(false);
            progressLabel.setVisible(false);
            timeRemainingLabel.setVisible(false);
            fileInfoLabel.setText("Gửi file thất bại: " + reason);
            btnSendFile.setDisable(false);
            Stage stage = (Stage) fileInfoLabel.getScene().getWindow();
            stage.close();
            if (mainController != null) {
                mainController.showMainScreen();
            }
        });
    }

    @FXML
    private void onSelectFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Chọn file để gửi");

        Stage stage = (Stage) fileInfoLabel.getScene().getWindow();
        selectedFile = fileChooser.showOpenDialog(stage);

        if (selectedFile != null) {
            fileInfoLabel.setText("File: " + selectedFile.getName() + " (Size: " + selectedFile.length() + " bytes)");
            btnEncryptFile.setDisable(false);
        }
    }

    @FXML
    private void onEncryptFile() {
        if (selectedFile != null) {
            try {
                progressBar.setProgress(0);
                progressBar.setVisible(true);
                progressLabel.setVisible(true);
                timeRemainingLabel.setVisible(true);
                btnEncryptFile.setDisable(true);

                fileInfoLabel.setText("Đang mã hóa file: " + selectedFile.getName());

                new Thread(() -> {
                    try {
                        fileTransfer.prepareForSending(selectedFile);
                        encryptedFile = fileTransfer.getEncryptedFile();

                        Platform.runLater(() -> {
                            fileInfoLabel.setText("File đã mã hóa: " + selectedFile.getName());
                            btnSendFile.setDisable(false);
                        });
                    } catch (Exception e) {
                        Log.error(e);
                        Platform.runLater(() -> {
                            fileInfoLabel.setText("Lỗi khi mã hóa: " + e.getMessage());
                            btnEncryptFile.setDisable(false);
                        });
                    }
                }).start();
            } catch (Exception e) {
                Log.error(e);
                fileInfoLabel.setText("Lỗi: " + e.getMessage());
            }
        }
    }
}