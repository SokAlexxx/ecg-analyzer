package com.ecganalyzer.model;

public class SuspiciousSegment {

    private final int startSampleIndex;
    private final int endSampleIndex;
    private final double startTimeSeconds;
    private final double endTimeSeconds;
    private final String type;
    private final String description;

    public SuspiciousSegment(int startSampleIndex,
                             int endSampleIndex,
                             double startTimeSeconds,
                             double endTimeSeconds,
                             String type,
                             String description) {
        this.startSampleIndex = startSampleIndex;
        this.endSampleIndex = endSampleIndex;
        this.startTimeSeconds = startTimeSeconds;
        this.endTimeSeconds = endTimeSeconds;
        this.type = type;
        this.description = description;
    }

    public int getStartSampleIndex() {
        return startSampleIndex;
    }

    public int getEndSampleIndex() {
        return endSampleIndex;
    }

    public double getStartTimeSeconds() {
        return startTimeSeconds;
    }

    public double getEndTimeSeconds() {
        return endTimeSeconds;
    }

    public String getType() {
        return type;
    }

    public String getDescription() {
        return description;
    }
}
