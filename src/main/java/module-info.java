module com.neoh.smartcampusgui {
    requires javafx.controls;
    requires javafx.fxml;


    opens com.neoh.smartcampusgui to javafx.fxml;
    exports com.neoh.smartcampusgui;
}