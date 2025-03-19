package org.duynguyen.atttclient.presentation.widgets;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.duynguyen.atttclient.HelloApplication;

import java.util.Objects;

public class GlobalUI {
    public static void init(Stage primaryStage) {
        AnchorPane overlayRoot = new AnchorPane();

        VBox messageContainer = new VBox();
        messageContainer.setSpacing(10);
        messageContainer.setPadding(new Insets(10));
        messageContainer.setAlignment(Pos.BOTTOM_RIGHT);
        AnchorPane.setBottomAnchor(messageContainer, 20.0);
        AnchorPane.setRightAnchor(messageContainer, 20.0);

        VBox dialogContainer = new VBox();
        dialogContainer.setAlignment(Pos.CENTER);
        dialogContainer.setPadding(new Insets(20));
        AnchorPane.setTopAnchor(dialogContainer, 20.0);
        AnchorPane.setRightAnchor(dialogContainer, 20.0);

        overlayRoot.getChildren().addAll(messageContainer, dialogContainer);

        Scene originalScene = primaryStage.getScene();
        Pane originalRoot = (Pane) originalScene.getRoot();
        StackPane newRoot = new StackPane();
        newRoot.getChildren().addAll(originalRoot, overlayRoot);

        Scene newScene = new Scene(newRoot, originalScene.getWidth(), originalScene.getHeight());

        String cssPath = Objects.requireNonNull(HelloApplication.class.getResource("toast.css")).toExternalForm();
        newScene.getStylesheets().add(cssPath);

        String dialogCssPath = Objects.requireNonNull(HelloApplication.class.getResource("dialog.css")).toExternalForm();
        newScene.getStylesheets().add(dialogCssPath);

        newScene.getStylesheets().addAll(originalScene.getStylesheets());
        primaryStage.setScene(newScene);

        ToastMessage.setMessageContainer(messageContainer);
        FileTransferDialog.setDialogContainer(dialogContainer);
    }
}