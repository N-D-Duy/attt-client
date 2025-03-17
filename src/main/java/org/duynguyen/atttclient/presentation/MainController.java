package org.duynguyen.atttclient.presentation;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.Setter;
import org.duynguyen.atttclient.HelloApplication;
import org.duynguyen.atttclient.models.User;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.utils.Log;

import java.io.IOException;
import java.util.List;

public class MainController {
    @FXML
    private Label userInfoLabel;

    @Getter
    private static MainController instance;

    @FXML
    private ListView<User> onlineUsersListView;

    private Session session = Session.getInstance();

    @Getter
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
            disableUI();
            showWaitingPopup("Đang chờ phản hồi từ " + selectedUser.username() + "...");

            session.getService().sendHandShakeRequest(selectedUser.id());
        }
    }

    private void disableUI() {
        onlineUsersListView.setDisable(true);
    }

    private void enableUI() {
        onlineUsersListView.setDisable(false);
    }

    private Stage waitingStage;

    private void showWaitingPopup(String message) {
        Platform.runLater(() -> {
            if (waitingStage != null) {
                waitingStage.close();
            }

            Stage stage = new Stage();
            waitingStage = stage;
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Đang chờ...");

            VBox vbox = new VBox(10);
            vbox.setAlignment(Pos.CENTER);
            vbox.setPadding(new Insets(20));

            Label label = new Label(message);
            ProgressIndicator progressIndicator = new ProgressIndicator();

            vbox.getChildren().addAll(progressIndicator, label);
            Scene scene = new Scene(vbox, 300, 150);
            stage.setScene(scene);
            stage.show();
        });
    }

    public void onHandshakeSuccess() {
        Platform.runLater(() -> {
            enableUI();
            closeWaitingPopup();
            openFileSelectionScreen();
        });
    }

    public void onHandshakeFailed() {
        Platform.runLater(() -> {
            enableUI();
            closeWaitingPopup();
            showErrorDialog("Handshake thất bại. Vui lòng thử lại sau.");
        });
    }


    private void closeWaitingPopup() {
        Platform.runLater(() -> {
            if (waitingStage != null) {
                waitingStage.close();
            }
        });
    }

    private void openFileSelectionScreen() {
        try {
            FXMLLoader loader = new FXMLLoader(HelloApplication.class.getResource("file_selection.fxml"));
            Parent root = loader.load();

            Stage stage = new Stage();
            stage.setScene(new Scene(root));
            stage.setTitle("Select File to Send");
            stage.show();
        } catch (IOException e) {
            Log.error("Không thể mở màn hình chọn file: " + e.getMessage());
        }
    }

    public void showMainScreen() {
        Stage primaryStage = HelloApplication.getPrimaryStage();
                primaryStage.show();
        primaryStage.setIconified(false);
        primaryStage.toFront();
    }

    private void showErrorDialog(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Lỗi");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}


