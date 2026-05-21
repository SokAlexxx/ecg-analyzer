package com.ecganalyzer.util;

import javafx.scene.control.Alert;

public final class DialogUtils {

    private DialogUtils() {
    }

    public static void showInfo(
            String title,
            String header,
            String content
    ) {

        Alert alert = new Alert(
                Alert.AlertType.INFORMATION
        );

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.showAndWait();
    }

    public static void showError(
            String title,
            String header,
            String content
    ) {

        Alert alert = new Alert(
                Alert.AlertType.ERROR
        );

        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);

        alert.showAndWait();
    }
}