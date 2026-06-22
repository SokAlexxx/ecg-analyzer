package com.ecganalyzer.service;

import com.ecganalyzer.model.SignalWindow;

import java.util.Arrays;

public class SignalWindowService {

    public SignalWindow createWindow(double[] signal, int startIndex, int windowSize) {
        if (signal == null || signal.length == 0) {
            return new SignalWindow(0, 0, new double[0]);
        }

        int safeWindowSize = Math.max(100, windowSize);
        int safeStartIndex = Math.max(0, Math.min(startIndex, signal.length - 1));
        int endIndex = Math.min(safeStartIndex + safeWindowSize, signal.length);

        if (endIndex <= safeStartIndex) {
            safeStartIndex = Math.max(0, signal.length - safeWindowSize);
            endIndex = signal.length;
        }

        return new SignalWindow(
                safeStartIndex,
                endIndex,
                Arrays.copyOfRange(signal, safeStartIndex, endIndex)
        );
    }

    public int moveLeft(int currentStartIndex, int windowSize) {
        return Math.max(0, currentStartIndex - windowSize);
    }

    public int moveRight(double[] signal, int currentStartIndex, int windowSize) {
        if (signal == null || signal.length == 0) {
            return 0;
        }

        int maxStartIndex = Math.max(0, signal.length - windowSize);
        return Math.min(maxStartIndex, currentStartIndex + windowSize);
    }

    public int clampStartIndex(double[] signal, int startIndex, int windowSize) {
        if (signal == null || signal.length == 0) {
            return 0;
        }

        int maxStartIndex = Math.max(0, signal.length - windowSize);
        return Math.max(0, Math.min(startIndex, maxStartIndex));
    }

    public int zoomIn(int currentWindowSize) {
        return Math.max(500, currentWindowSize / 2);
    }

    public int zoomOut(double[] signal, int currentWindowSize) {
        if (signal == null || signal.length == 0) {
            return currentWindowSize;
        }

        return Math.min(signal.length, currentWindowSize * 2);
    }
}
