package org.duynguyen.atttclient;

import java.io.IOException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import lombok.Getter;
import org.duynguyen.atttclient.network.ConnectionManager;
import org.duynguyen.atttclient.network.Session;
import org.duynguyen.atttclient.presentation.widgets.GlobalUI;
import org.duynguyen.atttclient.utils.Log;

public class HelloApplication extends Application {
  @Getter public static Stage primaryStage;
  private static Session session;
  private static ConnectionManager connectionManager;

  @Override
  public void start(Stage stage) throws IOException {
    primaryStage = stage;
    FXMLLoader fxmlLoader = new FXMLLoader(HelloApplication.class.getResource("startup.fxml"));
    Scene scene = new Scene(fxmlLoader.load(), 320, 240);
    stage.setTitle("Hello!");
    stage.setScene(scene);
    GlobalUI.init(stage);
    stage.setOnCloseRequest(
            event -> {
              System.out.println("Closing application...");
              closeConnection();
            });
    stage.show();
    establishConnection();
  }

  public static void main(String[] args) {
    connectionManager = new ConnectionManager("20.243.124.24", 1690);
    launch();
  }

  private void establishConnection() {
    connectionManager.connect()
            .thenAccept(s -> {
              session = s;
            })
            .exceptionally(ex -> {
              Log.error("Failed to connect: " + ex.getMessage());
              return null;
            });
  }

  @Override
  public void stop() throws Exception {
    System.out.println("Application is stopping...");
    closeConnection();
    super.stop();
  }

  private void closeConnection() {
    if (connectionManager != null) {
      connectionManager.shutdown();
      System.out.println("Connection closed.");
    }
  }
}