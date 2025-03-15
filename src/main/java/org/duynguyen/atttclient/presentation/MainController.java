package org.duynguyen.atttclient.presentation;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import lombok.Getter;
import lombok.Setter;
import org.duynguyen.atttclient.models.User;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.Log;

import java.util.List;

public class MainController {
    @FXML
    private Label userInfoLabel;

    @Getter
    private static MainController instance;

    @FXML
    private ListView<User> onlineUsersListView;

    private Session session = Session.getInstance();

    private List<User> users;


    @FXML
    public void initialize() {
        instance = this;
        onlineUsersListView.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setDisable(false);
                    setStyle(""); 
                } else {
                    setText(user.username() + (isYourself(user) ? " (You)" : ""));

                    if (isYourself(user)) {
                        setDisable(true);
                        setStyle("-fx-background-color: #d3d3d3; -fx-text-fill: gray;"); 
                    } else {
                        setDisable(false);
                        setStyle("");
                    }
                }
            }
        });
    }

    private boolean isYourself(User user) {
        return user.username().equals(session.username);
    }


    public void setUsers(List<User> users) {
        this.users = users;
        updateUI();
    }

    private void updateUI() {
        if (users != null) {
            Platform.runLater(() -> {
                onlineUsersListView.getItems().clear();
                onlineUsersListView.getItems().addAll(users);
            });
        }
    }

    @FXML
    private void onSendFileButtonClick() {
        User selectedUser = onlineUsersListView.getSelectionModel().getSelectedItem();
        if (selectedUser != null) {
            Log.info("Sending file to: " + selectedUser.username());
        }
    }
}
