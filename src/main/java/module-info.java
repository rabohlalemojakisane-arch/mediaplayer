module com.example.mediaplayerapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.media;
    requires jaudiotagger;


    opens com.example.mediaplayerapp to javafx.fxml;
    exports com.example.mediaplayerapp;
}