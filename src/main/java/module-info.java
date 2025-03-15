module org.duynguyen.atttclient {
    requires javafx.controls;
    requires javafx.fxml;

    requires org.controlsfx.controls;
    requires org.kordamp.ikonli.javafx;
    requires static lombok;
    requires org.slf4j;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires java.desktop;

    opens org.duynguyen.atttclient to javafx.fxml;
    exports org.duynguyen.atttclient;
    exports org.duynguyen.atttclient.presentation;
    opens org.duynguyen.atttclient.presentation to javafx.fxml;
}