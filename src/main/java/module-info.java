module consumption {
    requires javafx.controls;
    requires com.google.gson;
    exports consumption;
    exports consumption.model;
    exports consumption.ui;
    exports consumption.data;
    opens consumption;
    opens consumption.model to com.google.gson;
    opens consumption.data to com.google.gson;
}