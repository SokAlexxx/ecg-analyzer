package com.ecganalyzer;

import com.ecganalyzer.config.AppConfig;
import com.ecganalyzer.view.ViewManager;
import javafx.application.Application;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {

    @Override
    public void start(Stage stage) throws Exception {
        Parent root = ViewManager.loadView("/fxml/main-view.fxml");

        Scene scene = new Scene(
                root,
                AppConfig.WINDOW_WIDTH,
                AppConfig.WINDOW_HEIGHT
        );

        scene.getStylesheets().add(
                MainApp.class
                        .getResource("/css/main.css")
                        .toExternalForm()
        );

        stage.setTitle(AppConfig.APPLICATION_TITLE);
        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch();
    }
}