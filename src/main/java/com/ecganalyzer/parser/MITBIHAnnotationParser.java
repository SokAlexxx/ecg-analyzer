package com.ecganalyzer.parser;

import com.ecganalyzer.model.ECGAnnotation;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MITBIHAnnotationParser {

    public List<ECGAnnotation> parse(File annotationFile) {
        List<ECGAnnotation> annotations = new ArrayList<>();

        if (annotationFile == null || !annotationFile.exists()) {
            return annotations;
        }

        try (FileInputStream inputStream = new FileInputStream(annotationFile)) {
            byte[] bytes = inputStream.readAllBytes();

            long sampleIndex = 0;

            for (int i = 0; i + 1 < bytes.length; i += 2) {
                int b1 = bytes[i] & 0xFF;
                int b2 = bytes[i + 1] & 0xFF;

                int interval = b1 + ((b2 & 0x03) << 8);
                int annotationCode = (b2 & 0xFC) >> 2;

                if (annotationCode == 0) {
                    break;
                }

                sampleIndex += interval;

                String type = getAnnotationType(annotationCode);
                String description = getAnnotationDescription(type);

                annotations.add(new ECGAnnotation(sampleIndex, type, description));
            }

            return annotations;

        } catch (IOException e) {
            throw new RuntimeException("Помилка читання .atr файлу.", e);
        }
    }

    private String getAnnotationType(int code) {
        return switch (code) {
            case 1 -> "N";
            case 2 -> "L";
            case 3 -> "R";
            case 4 -> "a";
            case 5 -> "V";
            case 6 -> "F";
            case 7 -> "J";
            case 8 -> "A";
            case 9 -> "S";
            case 10 -> "E";
            case 11 -> "j";
            case 12 -> "/";
            case 13 -> "Q";
            case 14 -> "~";
            case 16 -> "|";
            case 18 -> "s";
            case 19 -> "T";
            case 20 -> "*";
            case 21 -> "D";
            case 22 -> "=";
            case 23 -> "\"";
            case 24 -> "@";
            case 25 -> "B";
            case 26 -> "^";
            case 27 -> "t";
            case 28 -> "+";
            case 29 -> "u";
            case 30 -> "?";
            case 31 -> "!";
            case 32 -> "[";
            case 33 -> "]";
            case 34 -> "e";
            case 35 -> "n";
            case 36 -> "p";
            case 37 -> "x";
            case 38 -> "f";
            case 39 -> "(";
            case 40 -> ")";
            case 41 -> "r";
            default -> "UNKNOWN";
        };
    }

    private String getAnnotationDescription(String type) {
        return switch (type) {
            case "N" -> "Normal beat";
            case "L" -> "Left bundle branch block beat";
            case "R" -> "Right bundle branch block beat";
            case "A" -> "Atrial premature beat";
            case "a" -> "Aberrated atrial premature beat";
            case "V" -> "Premature ventricular contraction";
            case "F" -> "Fusion of ventricular and normal beat";
            case "/" -> "Paced beat";
            case "Q" -> "Unclassifiable beat";
            case "~" -> "Signal quality change";
            case "+" -> "Rhythm change";
            default -> "Other annotation";
        };
    }
}