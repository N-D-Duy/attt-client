package org.duynguyen.atttclient.presentation;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.duynguyen.atttclient.HelloApplication;
import org.duynguyen.atttclient.utils.Log;

import java.io.IOException;

public class RegisterController {

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmPasswordField;

    @FXML
    private void onRegisterButtonClick() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showAlert("Username and password cannot be empty");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showAlert("Passwords do not match");
            return;
        }

        
        if (registerUser(username, password)) {
            try {
                
                switchToLoginScreen();
            } catch (IOException e) {
                Log.error(e.getMessage());
            }
        } else {
            showAlert("Failed to register. Username might be taken.");
        }
    }

    @FXML
    private void onLoginLinkClick() {
        try {
            switchToLoginScreen();
        } catch (IOException e) {
            Log.error(e.getMessage());
        }
    }

    private boolean registerUser(String username, String password) {
        
        
        Log.info("Registering user: " + username);
        return true;
    }

    private void switchToLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) usernameField.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Registration Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}