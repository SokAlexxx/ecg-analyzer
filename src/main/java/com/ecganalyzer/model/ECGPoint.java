package com.ecganalyzer.model;

public class ECGPoint {

    private final double timeSeconds;
    private final double voltageMv;

    public ECGPoint(double timeSeconds, double voltageMv) {
        this.timeSeconds = timeSeconds;
        this.voltageMv = voltageMv;
    }

    public double getTimeSeconds() {
        return timeSeconds;
    }

    public double getVoltageMv() {
        return voltageMv;
    }
}