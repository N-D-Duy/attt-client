package org.duynguyen.atttclient.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import org.duynguyen.atttclient.models.User;
import org.duynguyen.atttclient.network.ServerConnection;
import org.duynguyen.atttclient.utils.Log;

import java.util.Properties;

public class MainController {
    @FXML
    private Label userInfoLabel;

    @FXML
    private ListView<User> onlineUsersListView;

    private ServerConnection serverConnection;

    @FXML
    public void initialize() {
        serverConnection = new ServerConnection();
    }

    public void setUserConfig(Properties userConfig) {
        String displayName = userConfig.getProperty("displayName", "");
        String uuid = userConfig.getProperty("uuid", "");

        if (displayName.isEmpty()) {
            displayName = "Người dùng ẩn danh";
        }

        userInfoLabel.setText("Xin chào, " + displayName);
        connectToServer(uuid, displayName);
    }

    private void connectToServer(String uuid, String displayName) {
        new Thread(() -> {
            boolean connected = serverConnection.connect(uuid, displayName);

            if (connected) {
                javafx.application.Platform.runLater(() -> {
                    updateOnlineUsersList(serverConnection.getOnlineUsers());
                });
            } else {
                Log.error("Failed to connect to server");
            }
        }).start();
    }

    private void updateOnlineUsersList(java.util.List<User> users) {
        onlineUsersListView.getItems().clear();
        onlineUsersListView.getItems().addAll(users);
    }

    @FXML
    private void onSendFileButtonClick() {
        User selectedUser = onlineUsersListView.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            Log.info(selectedUser.username());
        }
    }
}