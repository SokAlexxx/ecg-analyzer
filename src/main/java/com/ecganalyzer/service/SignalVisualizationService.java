package com.ecganalyzer.service;

import com.ecganalyzer.model.SignalWindow;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;

public class SignalVisualizationService {

    private static final int MAX_POINTS_ON_CHART = 2500;

    public void drawSignal(LineChart<Number, Number> chart,
                           NumberAxis xAxis,
                           NumberAxis yAxis,
                           SignalWindow window,
                           int samplingFrequency) {
        chart.getData().clear();

        if (window == null || window.size() == 0 || samplingFrequency <= 0) {
            return;
        }

        XYChart.Series<Number, Number> series = new XYChart.Series<>();
        series.setName("Channel 1");

        double[] samples = window.getSamples();
        int step = Math.max(1, samples.length / MAX_POINTS_ON_CHART);

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 0; i < samples.length; i += step) {
            int sampleIndex = window.getStartIndex() + i;
            double timeSeconds = (double) sampleIndex / samplingFrequency;
            double value = samples[i];

            min = Math.min(min, value);
            max = Math.max(max, value);

            series.getData().add(new XYChart.Data<>(timeSeconds, value));
        }

        chart.getData().add(series);

        double startTime = (double) window.getStartIndex() / samplingFrequency;
        double endTime = (double) window.getEndIndex() / samplingFrequency;

        xAxis.setAutoRanging(false);
        xAxis.setLowerBound(startTime);
        xAxis.setUpperBound(endTime);
        xAxis.setTickUnit(Math.max(1.0, (endTime - startTime) / 5.0));

        if (min < max) {
            double padding = Math.max(0.1, (max - min) * 0.1);
            yAxis.setAutoRanging(false);
            yAxis.setLowerBound(min - padding);
            yAxis.setUpperBound(max + padding);
            yAxis.setTickUnit(Math.max(0.1, (max - min + padding * 2.0) / 5.0));
        }
    }
}
