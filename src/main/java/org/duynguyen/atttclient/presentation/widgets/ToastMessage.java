package org.duynguyen.atttclient.presentation.widgets;

import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import lombok.Setter;

public class ToastMessage {
    @Setter
    private static VBox messageContainer;

    public static void showMessage(String message) {
        if (messageContainer == null) {
            System.out.println("Message container not set!");
            return;
        }

        Platform.runLater(() -> {
            StackPane toastContainer = new StackPane();
            toastContainer.setMaxWidth(300);
            toastContainer.setMinHeight(40);
            toastContainer.getStyleClass().add("toast-container");
            Label msgLabel = new Label("");
            msgLabel.getStyleClass().add("toast-message");
            msgLabel.setWrapText(true);
            msgLabel.setMaxWidth(280);
            msgLabel.setAlignment(Pos.CENTER_LEFT);
            toastContainer.getChildren().add(msgLabel);
            toastContainer.setAlignment(Pos.CENTER_LEFT);
            messageContainer.getChildren().add(toastContainer);
            toastContainer.setTranslateX(0);
            toastContainer.setOpacity(1.0);
            SequentialTransition sequence = getSequentialTransition(message, msgLabel, toastContainer);
            sequence.play();
        });
    }

    private static SequentialTransition getSequentialTransition(String message, Label msgLabel, StackPane toastContainer) {
        PauseTransition showEmptyBox = new PauseTransition(Duration.seconds(0.5));
        showEmptyBox.setOnFinished(e -> msgLabel.setText(message));

        TranslateTransition textAnimation = new TranslateTransition(Duration.millis(800), msgLabel);
        textAnimation.setFromX(280);
        textAnimation.setToX(0);

        SequentialTransition sequence = getSequentialTransition(toastContainer, showEmptyBox, textAnimation);

        sequence.setOnFinished(e -> messageContainer.getChildren().remove(toastContainer));
        return sequence;
    }

    private static SequentialTransition getSequentialTransition(StackPane toastContainer, PauseTransition showEmptyBox, TranslateTransition textAnimation) {
        PauseTransition displayPause = new PauseTransition(Duration.seconds(2));


        TranslateTransition slideOut = new TranslateTransition(Duration.millis(600), toastContainer);
        slideOut.setToX(-messageContainer.getWidth() - 300);


        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), toastContainer);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);


        ParallelTransition exit = new ParallelTransition(slideOut, fadeOut);


        return new SequentialTransition(
                showEmptyBox, textAnimation, displayPause, exit);
    }
}