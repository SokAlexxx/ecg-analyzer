package com.ecganalyzer.model;

import java.io.File;
import java.time.LocalDateTime;
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
    private final int[] channelOneSignal;
    private final int[] channelTwoSignal;
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
                     int[] channelOneSignal,
                     int[] channelTwoSignal,
                     List<ECGAnnotation> annotations) {
        this.recordName = recordName;
        this.headerFile = headerFile;
        this.signalFile = signalFile;
        this.annotationFile = annotationFile;
        this.channels = channels;
        this.samplingFrequency = samplingFrequency;
        this.sampleCount = sampleCount;
        this.leadNames = leadNames;
        this.channelOneSignal = channelOneSignal;
        this.channelTwoSignal = channelTwoSignal;
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

    public int[] getChannelOneSignal() {
        return channelOneSignal;
    }

    public int[] getChannelTwoSignal() {
        return channelTwoSignal;
    }

    public List<ECGAnnotation> getAnnotations() {
        return annotations;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }
}