package com.ecganalyzer.parser;

public class ECGSignalData {

    private final int[] channelOneAdc;
    private final int[] channelTwoAdc;
    private final double[] channelOneMv;
    private final double[] channelTwoMv;

    public ECGSignalData(int[] channelOneAdc,
                         int[] channelTwoAdc,
                         double[] channelOneMv,
                         double[] channelTwoMv) {
        this.channelOneAdc = channelOneAdc;
        this.channelTwoAdc = channelTwoAdc;
        this.channelOneMv = channelOneMv;
        this.channelTwoMv = channelTwoMv;
    }

    public int[] getChannelOneAdc() {
        return channelOneAdc;
    }

    public int[] getChannelTwoAdc() {
        return channelTwoAdc;
    }

    public double[] getChannelOneMv() {
        return channelOneMv;
    }

    public double[] getChannelTwoMv() {
        return channelTwoMv;
    }

    public int getSampleCount() {
        return channelOneAdc.length;
    }
}