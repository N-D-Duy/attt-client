package org.duynguyen.atttclient.presentation;

import javafx.animation.TranslateTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import lombok.Getter;
import org.duynguyen.atttclient.HelloApplication;
import org.duynguyen.atttclient.models.User;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.Log;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class StartupController {
    private static final String CONFIG_FILE = "config.properties";
    @FXML
    private ProgressBar progressBar;
    private Properties userConfig;
    private final Session session = Session.getInstance();
    @Getter
    public static StartupController instance;
    @FXML
    public void initialize() {
        userConfig = loadConfig();
        Platform.runLater(this::processLogin);
        instance = this;
    }

    private void processLogin(){
        boolean hasCredentials = Boolean.parseBoolean(userConfig.getProperty("rememberMe", "false")) &&
                !userConfig.getProperty("savedUsername", "").isEmpty();

        if (hasCredentials) {
            String username = userConfig.getProperty("savedUsername", "");
            String password = userConfig.getProperty("savedPassword", "");

            session.getService().login(username, password);
        } else {
            navigateToLoginScreen();
        }
    }

    private Properties loadConfig() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            props.setProperty("savedUsername", "");
            props.setProperty("savedPassword", "");
            props.setProperty("rememberMe", "false");
            saveConfig(props);
        } else {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                Log.error("Error loading config: " + e.getMessage());
            }
        }

        return props;
    }

    private void saveConfig(Properties props) {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "User Configuration");
        } catch (IOException e) {
            Log.error("Error saving config: " + e.getMessage());
        }
    }


    public void onLoginSuccess(List<User> users){
        Platform.runLater(()->navigateToMainScreen(users));
    }
    private void navigateToMainScreen(List<User> users) {
        try {
            switchToMainScreen(users);
        } catch (IOException e) {
            Log.error("Failed to navigate to main screen: " + e.getMessage());
        }
    }
    private void switchToMainScreen(List<User> users) throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));
        Parent root = loader.load();

        MainController mainController = loader.getController();
        mainController.setUsers(users);

        Stage stage = (Stage) HelloApplication.getPrimaryStage().getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Secure File Transfer");
        stage.show();
    }
    public void onLoginFailed(){
        Platform.runLater(this::navigateToLoginScreen);
    }

    public void onLoginError(){
        Platform.runLater(this::navigateToLoginScreen);
    }

    private void navigateToLoginScreen() {
        try {
            switchToLoginScreen();
        } catch (IOException e) {
            Log.error("Failed to navigate to login screen: " + e.getMessage());
        }
    }



    private void switchToLoginScreen() throws IOException {
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("login.fxml"));
        Parent root = loader.load();

        Stage stage = (Stage) progressBar.getScene().getWindow();
        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Login");
        stage.show();
    }

    public void showDialog(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            Stage stage = (Stage) HelloApplication.primaryStage.getScene().getWindow();
            alert.initOwner(stage);
            alert.showAndWait();
        });
    }

}