package org.duynguyen.atttclient.presentation;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.duynguyen.atttclient.utils.Log;

public class FileTransferController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
        Log.info("Hello Button Clicked!");
    }
}
