package org.duynguyen.atttclient.presentation;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.duynguyen.atttclient.models.FileTransfer;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.Log;

import java.io.File;

public class FileSelectionController {
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

    @FXML
    public void initialize() {
        btnEncryptFile.setDisable(true);
        btnSendFile.setDisable(true);
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

    @FXML
    private void onSendFile() {
        if (encryptedFile != null) {
            session.getService().sendFileInfo(fileTransfer.getTransferId(), selectedFile.getName(), encryptedFile.length());
        }
    }
}

