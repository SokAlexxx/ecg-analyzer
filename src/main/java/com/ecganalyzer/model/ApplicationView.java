package com.ecganalyzer.model;

public enum ApplicationView {

    DASHBOARD("Dashboard", "General application overview"),
    VISUALIZATION("Visualization", "Module 3: ECG Signal Visualization"),
    SETTINGS("Settings", "Application settings placeholder");

    private final String title;
    private final String description;

    ApplicationView(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}
