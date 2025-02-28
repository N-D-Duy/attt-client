package org.duynguyen.atttclient.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

//@Slf4j
public class HelloController {
    @FXML
    private Label welcomeText;

    @FXML
    protected void onHelloButtonClick() {
        welcomeText.setText("Welcome to JavaFX Application!");
        Log.info( "HelloController.onHelloButtonClick()");
//        log.info( "HelloController.onHelloButtonClick()");
    }
}