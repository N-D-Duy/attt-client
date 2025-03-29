package org.duynguyen.atttclient.presentation;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import lombok.Setter;
import org.duynguyen.atttclient.protocol.FileTransfer;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.Log;

import java.io.File;

public class FileSelectionController implements FileTransfer.TransferCompleteListener{
    @FXML
    private Label fileInfoLabel;

    private final Session session = Session.getInstance();
    private final FileTransfer fileTransfer = FileTransfer.instance;

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

        if (fileTransfer != null) {
            fileTransfer.setTransferCompleteListener(this);
        }
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
        javafx.application.Platform.runLater(() -> {
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
        javafx.application.Platform.runLater(() -> {
            fileInfoLabel.setText("Gửi file thất bại: " + reason);
            btnSendFile.setDisable(false);
            Stage stage = (Stage) fileInfoLabel.getScene().getWindow();
            stage.close();
            if(mainController != null) {
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
                fileTransfer.prepareForSending(selectedFile);
                fileInfoLabel.setText("File: " + selectedFile.getName() + " (Size: " + selectedFile.length() + " bytes)");
                encryptedFile = fileTransfer.getEncryptedFile();
            } catch (Exception e) {
                Log.error(e);
            } finally {
                btnEncryptFile.setDisable(true);
                btnSendFile.setDisable(false);
            }
        }
    }
}

