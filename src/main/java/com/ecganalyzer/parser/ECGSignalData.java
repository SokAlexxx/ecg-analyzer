package com.ecganalyzer.parser;

public class ECGSignalData {

    private final int[] channelOne;
    private final int[] channelTwo;

    public ECGSignalData(int[] channelOne, int[] channelTwo) {
        this.channelOne = channelOne;
        this.channelTwo = channelTwo;
    }

    public int[] getChannelOne() {
        return channelOne;
    }

    public int[] getChannelTwo() {
        return channelTwo;
    }

    public int getSampleCount() {
        return channelOne.length;
    }
}