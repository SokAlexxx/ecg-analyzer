package com.ecganalyzer.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MITBIHHeaderParser {

    public MITBIHHeaderData parse(File headerFile) {
        if (headerFile == null || !headerFile.exists()) {
            throw new IllegalArgumentException("Файл заголовка .hea не знайдено.");
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(headerFile))) {
            String firstLine = reader.readLine();

            if (firstLine == null || firstLine.isBlank()) {
                throw new IllegalArgumentException("Файл .hea порожній.");
            }

            String[] mainParts = firstLine.trim().split("\\s+");

            if (mainParts.length < 3) {
                throw new IllegalArgumentException("Некоректний перший рядок .hea файлу.");
            }

            String recordName = mainParts[0];
            int channels = Integer.parseInt(mainParts[1]);
            int samplingFrequency = parseSamplingFrequency(mainParts[2]);
            long sampleCount = mainParts.length >= 4 ? Long.parseLong(mainParts[3]) : 0;

            List<String> leadNames = new ArrayList<>();

            for (int i = 0; i < channels; i++) {
                String signalLine = reader.readLine();

                if (signalLine == null || signalLine.isBlank()) {
                    continue;
                }

                String[] signalParts = signalLine.trim().split("\\s+");

                if (signalParts.length > 0) {
                    leadNames.add(signalParts[signalParts.length - 1]);
                }
            }

            return new MITBIHHeaderData(
                    recordName,
                    channels,
                    samplingFrequency,
                    sampleCount,
                    leadNames
            );

        } catch (IOException e) {
            throw new RuntimeException("Помилка читання .hea файлу.", e);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Некоректні числові дані у .hea файлі.", e);
        }
    }

    private int parseSamplingFrequency(String value) {
        if (value.contains("/")) {
            value = value.substring(0, value.indexOf("/"));
        }

        if (value.contains(".")) {
            return (int) Double.parseDouble(value);
        }

        return Integer.parseInt(value);
    }
}