package com.ecganalyzer.model;

public class ECGAnnotation {

    private final long sampleIndex;
    private final String type;
    private final String description;
    private final String source;

    public ECGAnnotation(long sampleIndex, String type, String description) {
        this(sampleIndex, type, description, "MIT-BIH");
    }

    public ECGAnnotation(long sampleIndex, String type, String description, String source) {
        this.sampleIndex = sampleIndex;
        this.type = type;
        this.description = description;
        this.source = source == null || source.isBlank() ? "MIT-BIH" : source;
    }

    public long getSampleIndex() {
        return sampleIndex;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }

    public String getSource() {
        return source;
    }

    public boolean isUserDefined() {
        return "User".equalsIgnoreCase(source);
    }
}
