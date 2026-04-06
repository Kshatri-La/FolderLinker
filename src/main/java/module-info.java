module com.filelinker {
    requires javafx.controls;
    requires javafx.fxml;

    opens com.filelinker to javafx.fxml;
    exports com.filelinker;
}
