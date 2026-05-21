package com.ecganalyzer.model;

public class ECGAnnotation {

    private final long sampleIndex;
    private final String type;
    private final String description;

    public ECGAnnotation(long sampleIndex, String type, String description) {
        this.sampleIndex = sampleIndex;
        this.type = type;
        this.description = description;
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
}