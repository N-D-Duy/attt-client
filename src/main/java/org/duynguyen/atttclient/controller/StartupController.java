package org.duynguyen.atttclient.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.duynguyen.atttclient.HelloApplication;
import org.duynguyen.atttclient.utils.Log;

import java.io.*;
import java.util.Properties;
import java.util.UUID;

public class StartupController {
    private static final String CONFIG_FILE = "config.properties";

    @FXML
    private ProgressBar progressBar;

    @FXML
    private TextField displayNameField;

    @FXML
    private Button continueButton;

    private Properties userConfig;
    private boolean isFirstRun = false;

    @FXML
    public void initialize() {
        // Tải hoặc tạo mới config
        userConfig = loadOrCreateConfig();

        if (isFirstRun) {
            // Nếu lần đầu chạy, hiện TextField để nhập tên hiển thị
            displayNameField.setVisible(true);
            continueButton.setVisible(true);
            progressBar.setVisible(false);
        } else {
            // Nếu không phải lần đầu, tự động chuyển sang màn hình chính
            displayNameField.setVisible(false);
            continueButton.setVisible(false);
            progressBar.setVisible(true);

            // Chuyển sang màn hình chính sau 1 giây
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                    javafx.application.Platform.runLater(() -> {
                        try {
                            switchToMainScreen();
                        } catch (IOException e) {
                            Log.error("Error switching to main screen: " + e.getMessage());
                        }
                    });
                } catch (InterruptedException e) {
                    Log.error("Startup delay interrupted: " + e.getMessage());
                }
            }).start();
        }
    }

    @FXML
    private void onContinueButtonClick() {
        // Lưu tên hiển thị vào config
        userConfig.setProperty("displayName", displayNameField.getText().trim());
        saveConfig();

        try {
            switchToMainScreen();
        } catch (IOException e) {
            Log.error("Error switching to main screen: " + e.getMessage());
        }
    }

    private Properties loadOrCreateConfig() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_FILE);

        if (!configFile.exists()) {
            isFirstRun = true;
            // Tạo UUID mới cho người dùng mới
            props.setProperty("uuid", UUID.randomUUID().toString());
            props.setProperty("displayName", "");
            saveConfig(props);
            System.out.println("Generated new UUID: " + props.getProperty("uuid"));
        } else {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);

                // Kiểm tra xem đã có tên hiển thị chưa
                String displayName = props.getProperty("displayName", "");
                if (displayName.isEmpty()) {
                    isFirstRun = true;
                }

                System.out.println("Loaded existing UUID: " + props.getProperty("uuid"));
                System.out.println("Display name: " + displayName);
            } catch (IOException e) {
                Log.error("Error loading config: " + e.getMessage());
                isFirstRun = true;
            }
        }

        return props;
    }

    private void saveConfig() {
        saveConfig(userConfig);
    }

    private void saveConfig(Properties props) {
        try (FileOutputStream fos = new FileOutputStream(CONFIG_FILE)) {
            props.store(fos, "User Configuration");
        } catch (IOException e) {
            Log.error("Error saving config: " + e.getMessage());
        }
    }

    private void switchToMainScreen() throws IOException {
        // Truyền các thông tin người dùng sang MainController
        FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("main.fxml"));
        Parent root = loader.load();

        // Lấy controller và cấu hình
        MainController mainController = loader.getController();
        mainController.setUserConfig(userConfig);

        // Lấy stage hiện tại từ một component đã hiện có trong scene
        Stage stage = (Stage) (isFirstRun ? continueButton.getScene().getWindow() :
                progressBar.getScene().getWindow());

        Scene scene = new Scene(root);
        stage.setScene(scene);
        stage.setTitle("Secure File Transfer");
        stage.show();
    }
}