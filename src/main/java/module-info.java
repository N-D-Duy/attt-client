module org.duynguyen.atttclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires org.apache.logging.log4j;
    requires static lombok;

    opens org.duynguyen.atttclient to javafx.fxml;
    exports org.duynguyen.atttclient;
    exports org.duynguyen.atttclient.controller;
    opens org.duynguyen.atttclient.controller to javafx.fxml;
}