package com.ecganalyzer.model;

public class ECGRecordSummary {

    private final int id;
    private final String recordName;
    private final int channels;
    private final int samplingFrequency;
    private final long sampleCount;
    private final String leadNames;
    private final int annotationsCount;
    private final String loadedAt;

    public ECGRecordSummary(int id,
                            String recordName,
                            int channels,
                            int samplingFrequency,
                            long sampleCount,
                            String leadNames,
                            int annotationsCount,
                            String loadedAt) {
        this.id = id;
        this.recordName = recordName;
        this.channels = channels;
        this.samplingFrequency = samplingFrequency;
        this.sampleCount = sampleCount;
        this.leadNames = leadNames;
        this.annotationsCount = annotationsCount;
        this.loadedAt = loadedAt;
    }

    public int getId() {
        return id;
    }

    public String getRecordName() {
        return recordName;
    }

    public int getChannels() {
        return channels;
    }

    public int getSamplingFrequency() {
        return samplingFrequency;
    }

    public long getSampleCount() {
        return sampleCount;
    }

    public String getLeadNames() {
        return leadNames;
    }

    public int getAnnotationsCount() {
        return annotationsCount;
    }

    public String getLoadedAt() {
        return loadedAt;
    }
}