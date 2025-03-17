package org.duynguyen.atttclient;

import java.io.IOException;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.presentation.widgets.GlobalUI;

public class HelloApplication extends Application {
  @Getter public static Stage primaryStage;

  @Override
  public void start(Stage stage) throws IOException {
    primaryStage = stage;
    FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("startup.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 320, 240);
    stage.setTitle("Hello!");
    stage.setScene(scene);
    GlobalUI.init(stage);
    stage.show();
  }

  public static void main(String[] args) {
            Session session = new Session("127.0.0.1", 1690);
//    Session session = new Session("20.243.124.24", 1690);
    session.connect();
    launch();
  }
}

