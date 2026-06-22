package com.ecganalyzer.service;

import com.ecganalyzer.model.ECGAnalysisResult;
import com.ecganalyzer.model.RPeak;
import com.ecganalyzer.model.SuspiciousSegment;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class SignalAnalysisService {

    private static final double MIN_R_PEAK_DISTANCE_SECONDS = 0.24;
    private static final double SEARCH_WINDOW_SECONDS = 0.055;
    private static final double SHORT_RR_RATIO = 0.62;
    private static final double LONG_RR_RATIO = 1.55;
    private static final double SHORT_RR_SECONDS = 0.43;
    private static final double LONG_RR_SECONDS = 1.50;
    private static final double OPPOSITE_DEFLECTION_RATIO = 1.25;
    private static final double MISSED_PEAK_GAP_RATIO = 1.42;

    public ECGAnalysisResult analyze(double[] signalMv, int samplingFrequency) {
        if (signalMv == null || signalMv.length == 0 || samplingFrequency <= 0) {
            return emptyResult();
        }

        BasicSignalStats stats = calculateSignalStats(signalMv);
        List<RPeak> rPeaks = detectRPeaks(signalMv, samplingFrequency);
        return buildResult(signalMv, samplingFrequency, rPeaks, stats);
    }

    public ECGAnalysisResult analyzeWithRPeaks(double[] signalMv, int samplingFrequency, List<RPeak> rPeaks) {
        if (signalMv == null || signalMv.length == 0 || samplingFrequency <= 0) {
            return emptyResult();
        }

        BasicSignalStats stats = calculateSignalStats(signalMv);
        List<RPeak> normalizedPeaks = normalizeRPeaks(signalMv, samplingFrequency, rPeaks);
        return buildResult(signalMv, samplingFrequency, normalizedPeaks, stats);
    }

    private ECGAnalysisResult buildResult(double[] signalMv,
                                          int samplingFrequency,
                                          List<RPeak> rPeaks,
                                          BasicSignalStats stats) {
        List<Double> rrIntervals = buildRrIntervals(rPeaks);

        double medianRr = median(rrIntervals);
        double averageRr = average(rrIntervals);
        double averageHeartRate = averageRr > 0.0 ? 60.0 / averageRr : 0.0;
        double minimumHeartRate = calculateMinimumHeartRate(rrIntervals);
        double maximumHeartRate = calculateMaximumHeartRate(rrIntervals);
        double sdnn = calculateSdnn(rrIntervals);
        double rmssd = calculateRmssd(rrIntervals);
        double pnn50 = calculatePnn50(rrIntervals);
        List<SuspiciousSegment> suspiciousSegments = detectSuspiciousSegments(signalMv, rPeaks, rrIntervals, medianRr, samplingFrequency, signalMv.length);
        String rhythmStatus = classifyRhythm(averageHeartRate, rrIntervals, medianRr, suspiciousSegments);

        return new ECGAnalysisResult(
                signalMv.length,
                (double) signalMv.length / samplingFrequency,
                stats.minimum(),
                stats.maximum(),
                stats.mean(),
                stats.standardDeviation(),
                new ArrayList<>(rPeaks),
                new ArrayList<>(rrIntervals),
                suspiciousSegments,
                averageRr,
                averageHeartRate,
                minimumHeartRate,
                maximumHeartRate,
                sdnn,
                rmssd,
                pnn50,
                rhythmStatus
        );
    }

    private ECGAnalysisResult emptyResult() {
        return new ECGAnalysisResult(
                0, 0.0, 0.0, 0.0, 0.0, 0.0,
                List.of(), List.of(), List.of(),
                0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0,
                "No signal"
        );
    }

    private BasicSignalStats calculateSignalStats(double[] signalMv) {
        double minimum = Double.MAX_VALUE;
        double maximum = -Double.MAX_VALUE;
        double sum = 0.0;

        for (double value : signalMv) {
            minimum = Math.min(minimum, value);
            maximum = Math.max(maximum, value);
            sum += value;
        }

        double mean = sum / signalMv.length;
        double varianceSum = 0.0;

        for (double value : signalMv) {
            double difference = value - mean;
            varianceSum += difference * difference;
        }

        double standardDeviation = Math.sqrt(varianceSum / signalMv.length);
        return new BasicSignalStats(minimum, maximum, mean, standardDeviation);
    }

    private List<RPeak> detectRPeaks(double[] signalMv, int samplingFrequency) {
        int minDistance = Math.max(1, (int) Math.round(MIN_R_PEAK_DISTANCE_SECONDS * samplingFrequency));
        int searchRadius = Math.max(2, (int) Math.round(SEARCH_WINDOW_SECONDS * samplingFrequency));

        double[] centered = removeLocalBaseline(signalMv, samplingFrequency);
        int dominantPolarity = estimateDominantPolarity(centered, samplingFrequency);
        double[] oriented = orientSignal(centered, dominantPolarity);

        List<Integer> maxima = collectSharpPositiveMaxima(oriented, samplingFrequency, false);
        if (maxima.isEmpty()) {
            return List.of();
        }

        double threshold = calculateRPeakAmplitudeThreshold(oriented, maxima);
        List<RPeak> candidates = new ArrayList<>();

        for (int sample : maxima) {
            if (oriented[sample] < threshold) {
                continue;
            }
            int peakSample = findDominantSample(oriented, sample, searchRadius);
            if (!hasSharpDominantQrs(oriented, peakSample, samplingFrequency, false)) {
                continue;
            }
            candidates.add(new RPeak(
                    peakSample,
                    (double) peakSample / samplingFrequency,
                    signalMv[peakSample]
            ));
        }

        List<RPeak> accepted = suppressClosePeaks(candidates, minDistance, dominantPolarity);
        accepted = repairMissedRPeaks(signalMv, oriented, samplingFrequency, accepted, dominantPolarity, threshold);
        return suppressClosePeaks(accepted, minDistance, dominantPolarity);
    }

    private List<Integer> collectSharpPositiveMaxima(double[] orientedSignal, int samplingFrequency, boolean relaxed) {
        List<Integer> maxima = new ArrayList<>();
        int localRadius = Math.max(1, (int) Math.round(0.018 * samplingFrequency));

        for (int i = localRadius; i < orientedSignal.length - localRadius; i++) {
            if (orientedSignal[i] <= 0.0) {
                continue;
            }

            boolean localMaximum = true;
            for (int j = i - localRadius; j <= i + localRadius; j++) {
                if (j != i && orientedSignal[j] > orientedSignal[i]) {
                    localMaximum = false;
                    break;
                }
            }
            if (!localMaximum) {
                continue;
            }

            if (hasSharpDominantQrs(orientedSignal, i, samplingFrequency, relaxed)) {
                maxima.add(i);
            }
        }
        return maxima;
    }

    private double calculateRPeakAmplitudeThreshold(double[] orientedSignal, List<Integer> maxima) {
        List<Double> amplitudes = maxima.stream()
                .map(index -> orientedSignal[index])
                .filter(value -> value > 0.0)
                .sorted(Double::compareTo)
                .toList();
        if (amplitudes.isEmpty()) {
            return 0.0;
        }

        double median = percentile(amplitudes, 0.50);
        double p75 = percentile(amplitudes, 0.75);
        double p90 = percentile(amplitudes, 0.90);

        return Math.max(0.06, Math.max(median * 0.72, Math.min(p75 * 0.70, p90 * 0.42)));
    }

    private List<RPeak> repairMissedRPeaks(double[] signalMv,
                                           double[] oriented,
                                           int samplingFrequency,
                                           List<RPeak> accepted,
                                           int dominantPolarity,
                                           double baseThreshold) {
        if (accepted.size() < 3) {
            return accepted;
        }

        List<RPeak> sorted = new ArrayList<>(accepted.stream()
                .sorted(Comparator.comparingInt(RPeak::getSampleIndex))
                .toList());
        double medianRr = median(buildRrIntervals(sorted));
        if (medianRr <= 0.0) {
            return sorted;
        }

        int expectedDistance = Math.max(1, (int) Math.round(medianRr * samplingFrequency));
        int minDistance = Math.max(1, (int) Math.round(MIN_R_PEAK_DISTANCE_SECONDS * samplingFrequency));
        int searchRadius = Math.max(2, (int) Math.round(Math.min(0.26, medianRr * 0.36) * samplingFrequency));
        double repairThreshold = Math.max(0.045, baseThreshold * 0.50);

        List<RPeak> repaired = new ArrayList<>(sorted);
        boolean changed = true;
        while (changed) {
            changed = false;
            repaired.sort(Comparator.comparingInt(RPeak::getSampleIndex));

            for (int i = 0; i < repaired.size() - 1; i++) {
                RPeak left = repaired.get(i);
                RPeak right = repaired.get(i + 1);
                int gap = right.getSampleIndex() - left.getSampleIndex();
                if (gap < expectedDistance * MISSED_PEAK_GAP_RATIO) {
                    continue;
                }

                int missingCount = Math.max(1, (int) Math.round((double) gap / expectedDistance) - 1);
                boolean insertedInGap = false;
                for (int m = 1; m <= missingCount; m++) {
                    int expectedSample = left.getSampleIndex() + (int) Math.round((double) gap * m / (missingCount + 1));
                    int candidate = findBestRepairCandidate(oriented, expectedSample, searchRadius, samplingFrequency, repairThreshold);
                    if (candidate < 0 || isNearDominantPeak(candidate, repaired, minDistance)) {
                        continue;
                    }

                    repaired.add(new RPeak(candidate, (double) candidate / samplingFrequency, signalMv[candidate]));
                    changed = true;
                    insertedInGap = true;
                }
                if (insertedInGap) {
                    break;
                }
            }
        }

        return suppressClosePeaks(repaired, minDistance, dominantPolarity);
    }

    private int findBestRepairCandidate(double[] oriented,
                                        int expectedSample,
                                        int searchRadius,
                                        int samplingFrequency,
                                        double repairThreshold) {
        int from = Math.max(1, expectedSample - searchRadius);
        int to = Math.min(oriented.length - 2, expectedSample + searchRadius);
        int best = -1;
        double bestScore = repairThreshold;

        for (int i = from; i <= to; i++) {
            if (oriented[i] < repairThreshold) {
                continue;
            }
            boolean localMaximum = oriented[i] >= oriented[i - 1] && oriented[i] >= oriented[i + 1];
            if (!localMaximum || !hasSharpDominantQrs(oriented, i, samplingFrequency, true)) {
                continue;
            }
            double distancePenalty = Math.abs(i - expectedSample) / (double) Math.max(1, searchRadius);
            double score = oriented[i] * (1.0 - 0.30 * distancePenalty);
            if (score > bestScore) {
                bestScore = score;
                best = i;
            }
        }
        return best;
    }

    private double[] removeLocalBaseline(double[] signalMv, int samplingFrequency) {
        double[] centered = new double[signalMv.length];
        int baselineRadius = Math.max(8, (int) Math.round(0.18 * samplingFrequency));
        for (int i = 0; i < signalMv.length; i++) {
            int from = Math.max(0, i - baselineRadius);
            int to = Math.min(signalMv.length - 1, i + baselineRadius);
            double baseline = 0.0;
            for (int j = from; j <= to; j++) {
                baseline += signalMv[j];
            }
            baseline /= (to - from + 1);
            centered[i] = signalMv[i] - baseline;
        }
        return centered;
    }

    private int estimateDominantPolarity(double[] centered, int samplingFrequency) {
        if (centered.length == 0) {
            return 1;
        }

        int window = Math.max(1, samplingFrequency);
        double positiveScore = 0.0;
        double negativeScore = 0.0;

        for (int start = 0; start < centered.length; start += window) {
            int end = Math.min(centered.length - 1, start + window - 1);
            double max = 0.0;
            double min = 0.0;
            for (int i = start; i <= end; i++) {
                max = Math.max(max, centered[i]);
                min = Math.min(min, centered[i]);
            }
            positiveScore += max;
            negativeScore += Math.abs(min);
        }

        return positiveScore >= negativeScore ? 1 : -1;
    }

    private double[] orientSignal(double[] centered, int dominantPolarity) {
        double[] oriented = new double[centered.length];
        for (int i = 0; i < centered.length; i++) {
            oriented[i] = dominantPolarity >= 0 ? centered[i] : -centered[i];
        }
        return oriented;
    }

    private double[] buildEnhancedSignal(double[] orientedSignal) {
        double[] enhanced = new double[orientedSignal.length];
        if (orientedSignal.length == 0) {
            return enhanced;
        }

        for (int i = 1; i < orientedSignal.length - 1; i++) {
            double positiveAmplitude = Math.max(0.0, orientedSignal[i]);
            double upwardSlope = Math.max(0.0, orientedSignal[i] - orientedSignal[i - 1]);
            double downwardSlope = Math.max(0.0, orientedSignal[i] - orientedSignal[i + 1]);
            enhanced[i] = positiveAmplitude * positiveAmplitude + 0.45 * upwardSlope * downwardSlope;
        }
        return enhanced;
    }

    private double adaptiveThreshold(double[] enhanced, int start, int end) {
        List<Double> values = new ArrayList<>();
        for (int i = start; i <= end; i++) {
            values.add(enhanced[i]);
        }
        values.sort(Double::compareTo);
        if (values.isEmpty()) {
            return 0.0;
        }

        double median = percentile(values, 0.50);
        double p90 = percentile(values, 0.90);
        double p98 = percentile(values, 0.98);
        return Math.max(median + (p90 - median) * 1.60, p98 * 0.62);
    }

    private double percentile(List<Double> sortedValues, double percentile) {
        if (sortedValues.isEmpty()) {
            return 0.0;
        }
        double position = percentile * (sortedValues.size() - 1);
        int lower = (int) Math.floor(position);
        int upper = (int) Math.ceil(position);
        if (lower == upper) {
            return sortedValues.get(lower);
        }
        double fraction = position - lower;
        return sortedValues.get(lower) * (1.0 - fraction) + sortedValues.get(upper) * fraction;
    }

    private boolean hasSharpDominantQrs(double[] orientedSignal, int sampleIndex, int samplingFrequency, boolean relaxed) {
        if (sampleIndex <= 0 || sampleIndex >= orientedSignal.length - 1) {
            return false;
        }

        double peak = orientedSignal[sampleIndex];
        if (peak <= 0.0) {
            return false;
        }

        int radius = Math.max(2, (int) Math.round(0.055 * samplingFrequency));
        int from = Math.max(0, sampleIndex - radius);
        int to = Math.min(orientedSignal.length - 1, sampleIndex + radius);

        double leftMinimum = peak;
        double rightMinimum = peak;
        double maxSlope = 0.0;

        for (int i = from + 1; i <= sampleIndex; i++) {
            leftMinimum = Math.min(leftMinimum, orientedSignal[i - 1]);
            maxSlope = Math.max(maxSlope, Math.abs(orientedSignal[i] - orientedSignal[i - 1]));
        }
        for (int i = sampleIndex + 1; i <= to; i++) {
            rightMinimum = Math.min(rightMinimum, orientedSignal[i]);
            maxSlope = Math.max(maxSlope, Math.abs(orientedSignal[i] - orientedSignal[i - 1]));
        }

        double prominence = peak - Math.max(leftMinimum, rightMinimum);
        double prominenceRatio = relaxed ? 0.22 : 0.30;
        double slopeRatio = relaxed ? 0.11 : 0.16;
        double minimumProminence = relaxed ? 0.045 : 0.065;
        double minimumSlope = relaxed ? 0.020 : 0.030;
        return prominence >= Math.max(minimumProminence, peak * prominenceRatio)
                && maxSlope >= Math.max(minimumSlope, peak * slopeRatio);
    }

    private int findDominantSample(double[] orientedSignal, int sampleIndex, int radius) {
        int from = Math.max(0, sampleIndex - radius);
        int to = Math.min(orientedSignal.length - 1, sampleIndex + radius);
        int best = sampleIndex;
        double bestScore = orientedSignal[sampleIndex];

        for (int i = from; i <= to; i++) {
            if (orientedSignal[i] > bestScore) {
                bestScore = orientedSignal[i];
                best = i;
            }
        }
        return best;
    }

    private List<RPeak> normalizeRPeaks(double[] signalMv, int samplingFrequency, List<RPeak> rPeaks) {
        if (rPeaks == null || rPeaks.isEmpty()) {
            return List.of();
        }
        int minDistance = Math.max(1, (int) Math.round(MIN_R_PEAK_DISTANCE_SECONDS * samplingFrequency));
        int dominantPolarity = estimateDominantPolarity(removeLocalBaseline(signalMv, samplingFrequency), samplingFrequency);
        List<RPeak> candidates = rPeaks.stream()
                .filter(peak -> peak.getSampleIndex() >= 0)
                .filter(peak -> peak.getSampleIndex() < signalMv.length)
                .map(peak -> new RPeak(
                        peak.getSampleIndex(),
                        (double) peak.getSampleIndex() / samplingFrequency,
                        signalMv[peak.getSampleIndex()]
                ))
                .sorted(Comparator.comparingInt(RPeak::getSampleIndex))
                .toList();
        return suppressClosePeaks(candidates, minDistance, dominantPolarity);
    }

    private List<RPeak> suppressClosePeaks(List<RPeak> candidates, int minDistance, int dominantPolarity) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<RPeak> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(RPeak::getSampleIndex))
                .toList();
        List<RPeak> accepted = new ArrayList<>();

        for (RPeak candidate : sorted) {
            if (accepted.isEmpty()) {
                accepted.add(candidate);
                continue;
            }

            RPeak last = accepted.get(accepted.size() - 1);
            if (candidate.getSampleIndex() - last.getSampleIndex() < minDistance) {
                if (dominantPolarity * candidate.getAmplitudeMv() > dominantPolarity * last.getAmplitudeMv()) {
                    accepted.set(accepted.size() - 1, candidate);
                }
            } else {
                accepted.add(candidate);
            }
        }

        return accepted;
    }

    private List<Double> buildRrIntervals(List<RPeak> peaks) {
        List<Double> rrIntervals = new ArrayList<>();

        for (int i = 1; i < peaks.size(); i++) {
            rrIntervals.add(peaks.get(i).getTimeSeconds() - peaks.get(i - 1).getTimeSeconds());
        }

        return rrIntervals;
    }

    private List<SuspiciousSegment> detectSuspiciousSegments(double[] signalMv,
                                                             List<RPeak> peaks,
                                                             List<Double> rrIntervals,
                                                             double medianRr,
                                                             int samplingFrequency,
                                                             int totalSamples) {
        List<SuspiciousSegment> segments = new ArrayList<>();
        if (peaks == null || peaks.size() < 4 || rrIntervals == null || rrIntervals.size() < 3 || medianRr <= 0.0) {
            return segments;
        }

        addRhythmBasedSuspiciousSegments(segments, peaks, rrIntervals, medianRr, samplingFrequency, totalSamples);
        addOppositeDeflectionSegments(segments, signalMv, peaks, samplingFrequency, totalSamples);

        return keepSeparateSuspiciousSegments(segments);
    }

    private void addRhythmBasedSuspiciousSegments(List<SuspiciousSegment> segments,
                                                  List<RPeak> peaks,
                                                  List<Double> rrIntervals,
                                                  double medianRr,
                                                  int samplingFrequency,
                                                  int totalSamples) {
        for (int i = 0; i < rrIntervals.size(); i++) {
            double rr = rrIntervals.get(i);
            RPeak previous = peaks.get(i);
            RPeak current = peaks.get(i + 1);
            String type = null;
            String description = null;

            double localReference = localRrReference(rrIntervals, i, medianRr);
            boolean veryLong = rr >= LONG_RR_SECONDS || rr / localReference >= LONG_RR_RATIO;
            boolean veryShort = rr <= SHORT_RR_SECONDS || rr / localReference <= SHORT_RR_RATIO;

            if (veryLong) {
                type = "Long RR";
                description = "Interval between detected R-peaks is markedly longer than the local rhythm";
            } else if (veryShort) {
                type = "Short RR";
                description = "Interval between detected R-peaks is markedly shorter than the local rhythm";
            }

            if (type != null) {
                addSegmentAroundRange(segments, previous.getSampleIndex(), current.getSampleIndex(), samplingFrequency, totalSamples, type, description);
            }
        }
    }

    private double localRrReference(List<Double> rrIntervals, int index, double fallback) {
        int from = Math.max(0, index - 3);
        int to = Math.min(rrIntervals.size() - 1, index + 3);
        List<Double> local = new ArrayList<>();
        for (int i = from; i <= to; i++) {
            if (i != index && rrIntervals.get(i) > 0.0) {
                local.add(rrIntervals.get(i));
            }
        }
        double value = median(local);
        return value > 0.0 ? value : fallback;
    }

    private void addOppositeDeflectionSegments(List<SuspiciousSegment> segments,
                                               double[] signalMv,
                                               List<RPeak> peaks,
                                               int samplingFrequency,
                                               int totalSamples) {
        List<Double> dominantPeakAmplitudes = peaks.stream()
                .map(peak -> Math.abs(peak.getAmplitudeMv()))
                .filter(value -> value > 0.0)
                .sorted(Double::compareTo)
                .toList();
        if (dominantPeakAmplitudes.size() < 4) {
            return;
        }

        double medianPeakAmplitude = percentile(dominantPeakAmplitudes, 0.50);
        if (medianPeakAmplitude <= 0.0) {
            return;
        }

        double[] centered = removeLocalBaseline(signalMv, samplingFrequency);
        int dominantPolarity = estimateDominantPolarity(centered, samplingFrequency);
        double[] oppositeOriented = new double[centered.length];
        for (int i = 0; i < centered.length; i++) {
            oppositeOriented[i] = dominantPolarity >= 0 ? -centered[i] : centered[i];
        }

        double threshold = medianPeakAmplitude * OPPOSITE_DEFLECTION_RATIO;
        int minDistance = Math.max(1, (int) Math.round(0.45 * samplingFrequency));
        int lastAccepted = -minDistance;

        for (int i = 1; i < oppositeOriented.length - 1; i++) {
            boolean localMaximum = oppositeOriented[i] > oppositeOriented[i - 1] && oppositeOriented[i] >= oppositeOriented[i + 1];
            if (!localMaximum || oppositeOriented[i] < threshold) {
                continue;
            }
            if (isNearDominantPeak(i, peaks, (int) Math.round(0.16 * samplingFrequency))) {
                continue;
            }
            if (i - lastAccepted < minDistance) {
                continue;
            }
            int margin = Math.max(1, (int) Math.round(0.22 * samplingFrequency));
            addSegmentAroundRange(segments,
                    i - margin,
                    i + margin,
                    samplingFrequency,
                    totalSamples,
                    "Opposite QRS deflection",
                    "Large QRS-like deflection has opposite polarity compared with the dominant detected rhythm");
            lastAccepted = i;
        }
    }

    private boolean isNearDominantPeak(int sampleIndex, List<RPeak> peaks, int radius) {
        for (RPeak peak : peaks) {
            if (Math.abs(peak.getSampleIndex() - sampleIndex) <= radius) {
                return true;
            }
            if (peak.getSampleIndex() > sampleIndex + radius) {
                return false;
            }
        }
        return false;
    }

    private void addSegmentAroundRange(List<SuspiciousSegment> segments,
                                       int startSample,
                                       int endSample,
                                       int samplingFrequency,
                                       int totalSamples,
                                       String type,
                                       String description) {
        int margin = Math.max(1, samplingFrequency / 18);
        int from = Math.max(0, Math.min(startSample, endSample) - margin);
        int to = Math.min(totalSamples - 1, Math.max(startSample, endSample) + margin);
        segments.add(new SuspiciousSegment(
                from,
                to,
                (double) from / samplingFrequency,
                (double) to / samplingFrequency,
                type,
                description
        ));
    }

    private double relativeDifference(double first, double second) {
        double denominator = Math.max(Math.abs(first), Math.abs(second));
        if (denominator <= 0.0) {
            return 0.0;
        }
        return Math.abs(first - second) / denominator;
    }

    private List<SuspiciousSegment> mergeOverlappingSegments(List<SuspiciousSegment> source, int samplingFrequency) {
        if (source.isEmpty()) {
            return source;
        }

        List<SuspiciousSegment> sorted = source.stream()
                .sorted(Comparator.comparingInt(SuspiciousSegment::getStartSampleIndex))
                .toList();
        List<SuspiciousSegment> merged = new ArrayList<>();
        SuspiciousSegment current = sorted.get(0);

        for (int i = 1; i < sorted.size(); i++) {
            SuspiciousSegment next = sorted.get(i);
            int mergeGap = Math.max(1, (int) Math.round(0.12 * samplingFrequency));
            if (next.getStartSampleIndex() <= current.getEndSampleIndex() + mergeGap) {
                int start = current.getStartSampleIndex();
                int end = Math.max(current.getEndSampleIndex(), next.getEndSampleIndex());
                String type = current.getType().equals(next.getType()) ? current.getType() : "Mixed changes";
                String description = current.getDescription().equals(next.getDescription())
                        ? current.getDescription()
                        : current.getDescription() + "; " + next.getDescription();
                current = new SuspiciousSegment(start, end, (double) start / samplingFrequency, (double) end / samplingFrequency, type, description);
            } else {
                merged.add(current);
                current = next;
            }
        }

        merged.add(current);
        return merged;
    }

    private List<SuspiciousSegment> keepSeparateSuspiciousSegments(List<SuspiciousSegment> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }
        return source.stream()
                .sorted(Comparator.comparingInt(SuspiciousSegment::getStartSampleIndex))
                .toList();
    }

    private String classifyRhythm(double averageHeartRate,
                                  List<Double> rrIntervals,
                                  double medianRr,
                                  List<SuspiciousSegment> suspiciousSegments) {
        if (rrIntervals == null || rrIntervals.isEmpty()) {
            return "Not enough R-peaks";
        }

        boolean irregular = medianRr > 0.0 && rrIntervals.stream()
                .anyMatch(rr -> Math.abs(rr - medianRr) / medianRr >= LONG_RR_RATIO - 1.0);

        if (averageHeartRate < 60.0) {
            return irregular ? "Bradycardic irregular rhythm" : "Bradycardic rhythm";
        }
        if (averageHeartRate > 100.0) {
            return irregular ? "Tachycardic irregular rhythm" : "Tachycardic rhythm";
        }
        if (irregular || !suspiciousSegments.isEmpty()) {
            return "Irregular rhythm signs";
        }
        return "Regular rhythm signs";
    }

    private double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }

        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }

        return sum / values.size();
    }

    private double median(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return 0.0;
        }
        List<Double> sorted = new ArrayList<>(values);
        sorted.sort(Double::compareTo);
        int middle = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(middle);
        }
        return (sorted.get(middle - 1) + sorted.get(middle)) / 2.0;
    }

    private double calculateMinimumHeartRate(List<Double> rrIntervals) {
        if (rrIntervals == null || rrIntervals.isEmpty()) {
            return 0.0;
        }

        double maxRr = rrIntervals.stream().mapToDouble(Double::doubleValue).max().orElse(0.0);
        return maxRr > 0.0 ? 60.0 / maxRr : 0.0;
    }

    private double calculateMaximumHeartRate(List<Double> rrIntervals) {
        if (rrIntervals == null || rrIntervals.isEmpty()) {
            return 0.0;
        }

        double minRr = rrIntervals.stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        return minRr > 0.0 ? 60.0 / minRr : 0.0;
    }

    private double calculateSdnn(List<Double> rrIntervals) {
        if (rrIntervals == null || rrIntervals.size() < 2) {
            return 0.0;
        }

        double mean = average(rrIntervals);
        double varianceSum = 0.0;

        for (double interval : rrIntervals) {
            double difference = interval - mean;
            varianceSum += difference * difference;
        }

        return Math.sqrt(varianceSum / (rrIntervals.size() - 1)) * 1000.0;
    }

    private double calculateRmssd(List<Double> rrIntervals) {
        if (rrIntervals == null || rrIntervals.size() < 2) {
            return 0.0;
        }

        double squaredDifferenceSum = 0.0;

        for (int i = 1; i < rrIntervals.size(); i++) {
            double difference = rrIntervals.get(i) - rrIntervals.get(i - 1);
            squaredDifferenceSum += difference * difference;
        }

        return Math.sqrt(squaredDifferenceSum / (rrIntervals.size() - 1)) * 1000.0;
    }

    private double calculatePnn50(List<Double> rrIntervals) {
        if (rrIntervals == null || rrIntervals.size() < 2) {
            return 0.0;
        }

        int aboveThreshold = 0;
        int comparisons = rrIntervals.size() - 1;

        for (int i = 1; i < rrIntervals.size(); i++) {
            double differenceMilliseconds = Math.abs(rrIntervals.get(i) - rrIntervals.get(i - 1)) * 1000.0;
            if (differenceMilliseconds > 50.0) {
                aboveThreshold++;
            }
        }

        return comparisons > 0 ? (aboveThreshold * 100.0) / comparisons : 0.0;
    }

    private record BasicSignalStats(double minimum, double maximum, double mean, double standardDeviation) {
    }
}
