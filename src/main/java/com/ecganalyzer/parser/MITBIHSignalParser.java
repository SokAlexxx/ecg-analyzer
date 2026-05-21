package com.ecganalyzer.parser;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class MITBIHSignalParser {

    public ECGSignalData parseFormat212(File signalFile, long expectedSamples) {
        if (signalFile == null || !signalFile.exists()) {
            throw new IllegalArgumentException("Файл сигналу .dat не знайдено.");
        }

        try (FileInputStream inputStream = new FileInputStream(signalFile)) {
            byte[] bytes = inputStream.readAllBytes();

            int pairCount = bytes.length / 3;

            int sampleLimit = expectedSamples > 0
                    ? (int) Math.min(expectedSamples, pairCount)
                    : pairCount;

            int[] channelOne = new int[sampleLimit];
            int[] channelTwo = new int[sampleLimit];

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

                channelOne[i] = sample1;
                channelTwo[i] = sample2;
            }

            return new ECGSignalData(channelOne, channelTwo);

        } catch (IOException e) {
            throw new RuntimeException("Помилка читання .dat файлу.", e);
        }
    }
}