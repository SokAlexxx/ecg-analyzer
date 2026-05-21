package com.ecganalyzer.parser;

import java.util.List;

public class MITBIHHeaderData {

    private final String recordName;
    private final int channels;
    private final int samplingFrequency;
    private final long sampleCount;
    private final List<String> leadNames;

    public MITBIHHeaderData(String recordName,
                            int channels,
                            int samplingFrequency,
                            long sampleCount,
                            List<String> leadNames) {
        this.recordName = recordName;
        this.channels = channels;
        this.samplingFrequency = samplingFrequency;
        this.sampleCount = sampleCount;
        this.leadNames = leadNames;
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

    public List<String> getLeadNames() {
        return leadNames;
    }
}