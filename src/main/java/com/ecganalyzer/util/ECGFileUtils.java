package com.ecganalyzer.util;

import java.io.File;

public class ECGFileUtils {

    private ECGFileUtils() {
    }

    public static String getBaseName(File file) {
        String name = file.getName();
        int dotIndex = name.lastIndexOf(".");
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    public static File findRelatedFile(File selectedFile, String extension) {
        String baseName = getBaseName(selectedFile);
        File directory = selectedFile.getParentFile();

        return new File(directory, baseName + extension);
    }
}