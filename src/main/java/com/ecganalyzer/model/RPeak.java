package com.ecganalyzer.model;

public class RPeak {

    private final int sampleIndex;
    private final double timeSeconds;
    private final double amplitudeMv;

    public RPeak(int sampleIndex, double timeSeconds, double amplitudeMv) {
        this.sampleIndex = sampleIndex;
        this.timeSeconds = timeSeconds;
        this.amplitudeMv = amplitudeMv;
    }

    public int getSampleIndex() {
        return sampleIndex;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public double getAmplitudeMv() {
        return amplitudeMv;
    }
}
