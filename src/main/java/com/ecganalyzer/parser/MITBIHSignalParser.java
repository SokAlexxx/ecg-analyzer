package com.ecganalyzer.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MITBIHSignalParser {

    public ECGSignalData parse(File signalFile, MITBIHHeaderData headerData) {
        int format = headerData.getPrimarySignalFormat();

        if (format != 212) {
            throw new IllegalArgumentException(
                    "Поки підтримується лише MIT-BIH формат 212. Поточний формат: " + format
            );
        }

        return parseFormat212(signalFile, headerData);
    }

    private ECGSignalData parseFormat212(File signalFile, MITBIHHeaderData headerData) {
        if (signalFile == null || !signalFile.exists()) {
            throw new IllegalArgumentException("Файл сигналу .dat не знайдено.");
        }

        try (FileInputStream inputStream = new FileInputStream(signalFile)) {
            byte[] bytes = inputStream.readAllBytes();

            int pairCount = bytes.length / 3;

            int sampleLimit = headerData.getSampleCount() > 0
                    ? (int) Math.min(headerData.getSampleCount(), pairCount)
                    : pairCount;

            int[] channelOneAdc = new int[sampleLimit];
            int[] channelTwoAdc = new int[sampleLimit];
            double[] channelOneMv = new double[sampleLimit];
            double[] channelTwoMv = new double[sampleLimit];

            double gainOne = headerData.getGainForChannel(0);
            double gainTwo = headerData.getGainForChannel(1);

            int baselineOne = headerData.getBaselineForChannel(0);
            int baselineTwo = headerData.getBaselineForChannel(1);

            for (int i = 0; i < sampleLimit; i++) {
                int byteIndex = i * 3;

                int b1 = bytes[byteIndex] & 0xFF;
                int b2 = bytes[byteIndex + 1] & 0xFF;
                int b3 = bytes[byteIndex + 2] & 0xFF;

                int sample1 = ((b2 & 0x0F) << 8) | b1;
                int sample2 = ((b2 & 0xF0) << 4) | b3;

                if ((sample1 & 0x800) != 0) {
                    sample1 -= 0x1000;
                }

                if ((sample2 & 0x800) != 0) {
                    sample2 -= 0x1000;
                }

                channelOneAdc[i] = sample1;
                channelTwoAdc[i] = sample2;

                channelOneMv[i] = convertAdcToMv(sample1, baselineOne, gainOne);
                channelTwoMv[i] = convertAdcToMv(sample2, baselineTwo, gainTwo);
            }

            return new ECGSignalData(
                    channelOneAdc,
                    channelTwoAdc,
                    channelOneMv,
                    channelTwoMv
            );

        } catch (IOException e) {
            throw new RuntimeException("Помилка читання .dat файлу.", e);
        }
    }

    private double convertAdcToMv(int adcValue, int baseline, double gain) {
        if (gain == 0) {
            return adcValue;
        }

        return (adcValue - baseline) / gain;
    }
}