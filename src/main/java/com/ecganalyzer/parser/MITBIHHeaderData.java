package com.ecganalyzer.parser;

import java.util.List;

public class MITBIHHeaderData {

    private final String recordName;
    private final int channels;
    private final int samplingFrequency;
    private final long sampleCount;
    private final List<String> leadNames;
    private final List<Integer> signalFormats;
    private final List<Double> gains;
    private final List<Integer> baselines;

    public MITBIHHeaderData(String recordName,
                            int channels,
                            int samplingFrequency,
                            long sampleCount,
                            List<String> leadNames,
                            List<Integer> signalFormats,
                            List<Double> gains,
                            List<Integer> baselines) {
        this.recordName = recordName;
        this.channels = channels;
        this.samplingFrequency = samplingFrequency;
        this.sampleCount = sampleCount;
        this.leadNames = leadNames;
        this.signalFormats = signalFormats;
        this.gains = gains;
        this.baselines = baselines;
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

    public List<Integer> getSignalFormats() {
        return signalFormats;
    }

    public List<Double> getGains() {
        return gains;
    }

    public List<Integer> getBaselines() {
        return baselines;
    }

    public int getPrimarySignalFormat() {
        return signalFormats.isEmpty() ? 212 : signalFormats.get(0);
    }

    public double getGainForChannel(int channelIndex) {
        return gains.size() > channelIndex ? gains.get(channelIndex) : 200.0;
    }

    public int getBaselineForChannel(int channelIndex) {
        return baselines.size() > channelIndex ? baselines.get(channelIndex) : 0;
    }
}