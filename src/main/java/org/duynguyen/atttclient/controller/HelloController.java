package org.duynguyen.atttclient.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import lombok.extern.slf4j.Slf4j;
import org.duynguyen.atttclient.utils.Log;

public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
        Log.info("Hello Button Clicked!");
    }
}