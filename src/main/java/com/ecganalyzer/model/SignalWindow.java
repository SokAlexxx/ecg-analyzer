package com.ecganalyzer.model;

public class SignalWindow {

    private final int startIndex;
    private final int endIndex;
    private final double[] samples;

    public SignalWindow(int startIndex, int endIndex, double[] samples) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.samples = samples;
    }

    public int getStartIndex() {
        return startIndex;
    }

    public int getEndIndex() {
        return endIndex;
    }

    public double[] getSamples() {
        return samples;
    }

    public int size() {
        return samples.length;
    }
}
