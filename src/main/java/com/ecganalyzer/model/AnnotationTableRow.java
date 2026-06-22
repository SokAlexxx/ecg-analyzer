package com.ecganalyzer.model;

import javafx.beans.property.SimpleLongProperty;
import javafx.beans.property.SimpleStringProperty;

public class AnnotationTableRow {

    private final SimpleLongProperty sampleIndex;
    private final SimpleStringProperty timeSeconds;
    private final SimpleStringProperty type;
    private final SimpleStringProperty source;
    private final SimpleStringProperty description;

    public AnnotationTableRow(long sampleIndex, double timeSeconds, String type, String source, String description) {
        this.sampleIndex = new SimpleLongProperty(sampleIndex);
        this.timeSeconds = new SimpleStringProperty(String.format("%.3f", timeSeconds));
        this.type = new SimpleStringProperty(type);
        this.source = new SimpleStringProperty(source);
        this.description = new SimpleStringProperty(description);
    }

    public long getSampleIndex() {
        return sampleIndex.get();
    }

    public String getTimeSeconds() {
        return timeSeconds.get();
    }

    public String getType() {
        return type.get();
    }

    public String getSource() {
        return source.get();
    }

    public String getDescription() {
        return description.get();
    }
}
