package com.ecganalyzer.view;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.IOException;
import java.net.URL;

public final class ViewManager {

    private ViewManager() {
    }

    public static Parent loadView(String fxmlPath) throws IOException {
        URL resource = ViewManager.class.getResource(fxmlPath);

        if (resource == null) {
            throw new IOException("FXML resource not found: " + fxmlPath);
        }

        FXMLLoader loader = new FXMLLoader(resource);
        return loader.load();
    }
}