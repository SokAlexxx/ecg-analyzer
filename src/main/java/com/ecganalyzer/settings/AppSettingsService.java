package com.ecganalyzer.settings;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public class AppSettingsService {

    private static final String SETTINGS_DIRECTORY = "config";
    private static final String SETTINGS_FILE = "app-settings.properties";

    public AppSettings load() {
        AppSettings settings = new AppSettings();
        Path path = getSettingsPath();

        if (!Files.exists(path)) {
            return settings;
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(path)) {
            properties.load(inputStream);
            settings.setDefaultWindowSize(readInt(properties, "defaultWindowSize", settings.getDefaultWindowSize()));
            settings.setShowGridByDefault(readBoolean(properties, "showGridByDefault", settings.isShowGridByDefault()));
            settings.setShowAnnotationsByDefault(readBoolean(properties, "showAnnotationsByDefault", settings.isShowAnnotationsByDefault()));
            settings.setShowRPeaksByDefault(readBoolean(properties, "showRPeaksByDefault", settings.isShowRPeaksByDefault()));
            settings.setShowSuspiciousSegmentsByDefault(readBoolean(properties, "showSuspiciousSegmentsByDefault", settings.isShowSuspiciousSegmentsByDefault()));
            settings.setDefaultAnalysisMode(properties.getProperty("defaultAnalysisMode", settings.getDefaultAnalysisMode()));
        } catch (IOException exception) {
            return settings;
        }

        return settings;
    }

    public void save(AppSettings settings) throws IOException {
        Path path = getSettingsPath();
        Files.createDirectories(path.getParent());

        Properties properties = new Properties();
        properties.setProperty("defaultWindowSize", String.valueOf(settings.getDefaultWindowSize()));
        properties.setProperty("showGridByDefault", String.valueOf(settings.isShowGridByDefault()));
        properties.setProperty("showAnnotationsByDefault", String.valueOf(settings.isShowAnnotationsByDefault()));
        properties.setProperty("showRPeaksByDefault", String.valueOf(settings.isShowRPeaksByDefault()));
        properties.setProperty("showSuspiciousSegmentsByDefault", String.valueOf(settings.isShowSuspiciousSegmentsByDefault()));
        properties.setProperty("defaultAnalysisMode", settings.getDefaultAnalysisMode());

        try (OutputStream outputStream = Files.newOutputStream(path)) {
            properties.store(outputStream, "ECG Signal Analyzer settings");
        }
    }

    private Path getSettingsPath() {
        return Path.of(SETTINGS_DIRECTORY, SETTINGS_FILE);
    }

    private int readInt(Properties properties, String key, int fallback) {
        try {
            return Integer.parseInt(properties.getProperty(key, String.valueOf(fallback)));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private boolean readBoolean(Properties properties, String key, boolean fallback) {
        String value = properties.getProperty(key);
        return value == null ? fallback : Boolean.parseBoolean(value);
    }
}
