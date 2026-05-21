package com.ecganalyzer.model;

import java.io.File;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ECGRecord {

    private final String recordName;
    private final File headerFile;
    private final File signalFile;
    private final File annotationFile;

    private final int channels;
    private final int samplingFrequency;
    private final long sampleCount;

    private final List<String> leadNames;
    private final List<Integer> signalFormats;
    private final List<Double> gains;
    private final List<Integer> baselines;

    private final int[] channelOneSignal;
    private final int[] channelTwoSignal;

    private final double[] channelOneMv;
    private final double[] channelTwoMv;

    private final List<ECGAnnotation> annotations;
    private final LocalDateTime loadedAt;

    public ECGRecord(String recordName,
                     File headerFile,
                     File signalFile,
                     File annotationFile,
                     int channels,
                     int samplingFrequency,
                     long sampleCount,
                     List<String> leadNames,
                     List<Integer> signalFormats,
                     List<Double> gains,
                     List<Integer> baselines,
                     int[] channelOneSignal,
                     int[] channelTwoSignal,
                     double[] channelOneMv,
                     double[] channelTwoMv,
                     List<ECGAnnotation> annotations) {
        this.recordName = recordName;
        this.headerFile = headerFile;
        this.signalFile = signalFile;
        this.annotationFile = annotationFile;
        this.channels = channels;
        this.samplingFrequency = samplingFrequency;
        this.sampleCount = sampleCount;
        this.leadNames = leadNames;
        this.signalFormats = signalFormats;
        this.gains = gains;
        this.baselines = baselines;
        this.channelOneSignal = channelOneSignal;
        this.channelTwoSignal = channelTwoSignal;
        this.channelOneMv = channelOneMv;
        this.channelTwoMv = channelTwoMv;
        this.annotations = annotations;
        this.loadedAt = LocalDateTime.now();
    }

    public String getRecordName() {
        return recordName;
    }

    public File getHeaderFile() {
        return headerFile;
    }

    public File getSignalFile() {
        return signalFile;
    }

    public File getAnnotationFile() {
        return annotationFile;
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

    public int[] getChannelOneSignal() {
        return channelOneSignal;
    }

    public int[] getChannelTwoSignal() {
        return channelTwoSignal;
    }

    public double[] getChannelOneMv() {
        return channelOneMv;
    }

    public double[] getChannelTwoMv() {
        return channelTwoMv;
    }

    public List<ECGAnnotation> getAnnotations() {
        return annotations;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    public List<ECGPoint> getChannelOnePoints() {
        return buildPoints(channelOneMv);
    }

    public List<ECGPoint> getChannelTwoPoints() {
        return buildPoints(channelTwoMv);
    }

    private List<ECGPoint> buildPoints(double[] valuesMv) {
        List<ECGPoint> points = new ArrayList<>();

        if (valuesMv == null || samplingFrequency <= 0) {
            return points;
        }

        for (int i = 0; i < valuesMv.length; i++) {
            double timeSeconds = (double) i / samplingFrequency;
            points.add(new ECGPoint(timeSeconds, valuesMv[i]));
        }

        return points;
    }
}