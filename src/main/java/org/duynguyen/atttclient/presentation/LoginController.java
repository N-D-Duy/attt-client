package org.duynguyen.atttclient.presentation;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.CheckBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.duynguyen.atttclient.HelloApplication;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.Log;

import java.io.*;
import java.util.Properties;

public class LoginController {
    private static final String CONFIG_FILE = "config.properties";

    @FXML
    private TextField usernameField;

    private final Session session = Session.getInstance();

    @FXML
    private PasswordField passwordField;

    @FXML
    private CheckBox rememberMeCheckbox;

    @FXML
    public void initialize() {
        loadSavedCredentials();
        if (rememberMeCheckbox.isSelected() &&
                !usernameField.getText().isEmpty() &&
                !passwordField.getText().isEmpty()) {
            Alert autoLoginAlert = getAlert();
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(() -> {
                        autoLoginAlert.close();
                        onLoginButtonClick();
                    });
                } catch (InterruptedException e) {
                    Log.error("Auto login interrupted: " + e.getMessage());
                }
            }).start();
        }
    }

    private static Alert getAlert() {
        Alert autoLoginAlert = new Alert(Alert.AlertType.INFORMATION);
        autoLoginAlert.setTitle("Auto Login");
        autoLoginAlert.setHeaderText(null);
        autoLoginAlert.setContentText("Logging in automatically...");
        autoLoginAlert.setGraphic(new javafx.scene.control.ProgressIndicator());
        Stage stage = (Stage) HelloApplication.primaryStage.getScene().getWindow();
        autoLoginAlert.initOwner(stage);

        autoLoginAlert.show();
        return autoLoginAlert;
    }


    @FXML
    private void onLoginButtonClick() {
        String username = usernameField.getText();
        String password = passwordField.getText();

        session.getService().login(username, password);
        session.setSaveCredentialsListener(() -> {
            Platform.runLater(() -> {
                if (rememberMeCheckbox.isSelected()) {
                    saveCredentials(username, password);
                } else {
                    clearSavedCredentials();
                }
            });
        });
    }


    @FXML
    private void onRegisterLinkClick() {
        try {
            switchToRegisterScreen();
        } catch (IOException e) {
            Log.error(e.getMessage());
        }
    }

    private void saveCredentials(String username, String password) {
        try {
            Properties props = loadConfig();
            props.setProperty("savedUsername", username);
            props.setProperty("savedPassword", password);
            props.setProperty("rememberMe", "true");
            saveConfig(props);
        } catch (IOException e) {
            Log.error("Failed to save credentials: " + e.getMessage());
        }
    }

    private void clearSavedCredentials() {
        try {
            Properties props = loadConfig();
            props.remove("savedUsername");
            props.remove("savedPassword");
            props.setProperty("rememberMe", "false");
            saveConfig(props);
        } catch (IOException e) {
            Log.error("Failed to clear credentials: " + e.getMessage());
        }
    }

    private void loadSavedCredentials() {
        try {
            Properties props = loadConfig();
            String savedUsername = props.getProperty("savedUsername", "");
            String savedPassword = props.getProperty("savedPassword", "");
            boolean rememberMe = Boolean.parseBoolean(props.getProperty("rememberMe", "false"));

            if (rememberMe && !savedUsername.isEmpty()) {
                usernameField.setText(savedUsername);
                passwordField.setText(savedPassword);
                rememberMeCheckbox.setSelected(true);
            }
        } catch (IOException e) {
            Log.error("Failed to load saved credentials: " + e.getMessage());
        }
    }

    private Properties loadConfig() throws IOException {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            }
        }
        return props;
    }

    private void saveConfig(Properties props) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "User Configuration");
        }
    }

    private void switchToRegisterScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("register.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) usernameField.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Register");
        stage.show();
    }
}