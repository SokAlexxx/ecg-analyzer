package com.ecganalyzer.model;

import java.util.List;

public class ECGAnalysisResult {

    private final int totalSamples;
    private final double durationSeconds;
    private final double minimumAmplitudeMv;
    private final double maximumAmplitudeMv;
    private final double meanAmplitudeMv;
    private final double standardDeviationMv;
    private final List<RPeak> rPeaks;
    private final List<Double> rrIntervalsSeconds;
    private final List<SuspiciousSegment> suspiciousSegments;
    private final double averageRrIntervalSeconds;
    private final double averageHeartRateBpm;
    private final double minimumHeartRateBpm;
    private final double maximumHeartRateBpm;
    private final double sdnnMilliseconds;
    private final double rmssdMilliseconds;
    private final double pnn50Percent;
    private final String rhythmStatus;

    public ECGAnalysisResult(int totalSamples,
                             double durationSeconds,
                             double minimumAmplitudeMv,
                             double maximumAmplitudeMv,
                             double meanAmplitudeMv,
                             double standardDeviationMv,
                             List<RPeak> rPeaks,
                             List<Double> rrIntervalsSeconds,
                             List<SuspiciousSegment> suspiciousSegments,
                             double averageRrIntervalSeconds,
                             double averageHeartRateBpm,
                             double minimumHeartRateBpm,
                             double maximumHeartRateBpm,
                             double sdnnMilliseconds,
                             double rmssdMilliseconds,
                             double pnn50Percent,
                             String rhythmStatus) {
        this.totalSamples = totalSamples;
        this.durationSeconds = durationSeconds;
        this.minimumAmplitudeMv = minimumAmplitudeMv;
        this.maximumAmplitudeMv = maximumAmplitudeMv;
        this.meanAmplitudeMv = meanAmplitudeMv;
        this.standardDeviationMv = standardDeviationMv;
        this.rPeaks = rPeaks;
        this.rrIntervalsSeconds = rrIntervalsSeconds;
        this.suspiciousSegments = suspiciousSegments;
        this.averageRrIntervalSeconds = averageRrIntervalSeconds;
        this.averageHeartRateBpm = averageHeartRateBpm;
        this.minimumHeartRateBpm = minimumHeartRateBpm;
        this.maximumHeartRateBpm = maximumHeartRateBpm;
        this.sdnnMilliseconds = sdnnMilliseconds;
        this.rmssdMilliseconds = rmssdMilliseconds;
        this.pnn50Percent = pnn50Percent;
        this.rhythmStatus = rhythmStatus;
    }

    public int getTotalSamples() { return totalSamples; }
    public double getDurationSeconds() { return durationSeconds; }
    public double getMinimumAmplitudeMv() { return minimumAmplitudeMv; }
    public double getMaximumAmplitudeMv() { return maximumAmplitudeMv; }
    public double getMeanAmplitudeMv() { return meanAmplitudeMv; }
    public double getStandardDeviationMv() { return standardDeviationMv; }
    public List<RPeak> getRPeaks() { return rPeaks; }
    public List<Double> getRrIntervalsSeconds() { return rrIntervalsSeconds; }
    public List<SuspiciousSegment> getSuspiciousSegments() { return suspiciousSegments; }
    public double getAverageRrIntervalSeconds() { return averageRrIntervalSeconds; }
    public double getAverageHeartRateBpm() { return averageHeartRateBpm; }
    public double getMinimumHeartRateBpm() { return minimumHeartRateBpm; }
    public double getMaximumHeartRateBpm() { return maximumHeartRateBpm; }
    public double getSdnnMilliseconds() { return sdnnMilliseconds; }
    public double getRmssdMilliseconds() { return rmssdMilliseconds; }
    public double getPnn50Percent() { return pnn50Percent; }
    public String getRhythmStatus() { return rhythmStatus; }
}
