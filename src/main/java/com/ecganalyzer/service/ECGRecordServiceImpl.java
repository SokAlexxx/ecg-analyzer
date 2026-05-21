package com.ecganalyzer.service;

import com.ecganalyzer.model.ECGAnnotation;
import com.ecganalyzer.model.ECGRecord;
import com.ecganalyzer.parser.ECGSignalData;
import com.ecganalyzer.parser.MITBIHAnnotationParser;
import com.ecganalyzer.parser.MITBIHHeaderData;
import com.ecganalyzer.parser.MITBIHHeaderParser;
import com.ecganalyzer.parser.MITBIHSignalParser;
import com.ecganalyzer.repository.ECGRecordRepository;
import com.ecganalyzer.util.ECGFileUtils;

import java.io.File;
import java.util.List;

public class ECGRecordServiceImpl implements ECGRecordService {

    private final MITBIHHeaderParser headerParser = new MITBIHHeaderParser();
    private final MITBIHSignalParser signalParser = new MITBIHSignalParser();
    private final MITBIHAnnotationParser annotationParser = new MITBIHAnnotationParser();
    private final ECGRecordRepository recordRepository = new ECGRecordRepository();

    @Override
    public ECGRecord loadRecord(File selectedFile) {
        if (selectedFile == null) {
            throw new IllegalArgumentException("Файл не вибрано.");
        }

        File headerFile = ECGFileUtils.findRelatedFile(selectedFile, ".hea");
        File signalFile = ECGFileUtils.findRelatedFile(selectedFile, ".dat");
        File annotationFile = ECGFileUtils.findRelatedFile(selectedFile, ".atr");

        if (!headerFile.exists()) {
            throw new IllegalArgumentException("Не знайдено файл заголовка .hea.");
        }

        if (!signalFile.exists()) {
            throw new IllegalArgumentException("Не знайдено файл сигналу .dat.");
        }

        MITBIHHeaderData headerData = headerParser.parse(headerFile);
        ECGSignalData signalData = signalParser.parse(signalFile, headerData);

        List<ECGAnnotation> annotations = annotationParser.parse(
                annotationFile.exists() ? annotationFile : null
        );

        ECGRecord record = new ECGRecord(
                headerData.getRecordName(),
                headerFile,
                signalFile,
                annotationFile.exists() ? annotationFile : null,
                headerData.getChannels(),
                headerData.getSamplingFrequency(),
                headerData.getSampleCount(),
                headerData.getLeadNames(),
                headerData.getSignalFormats(),
                headerData.getGains(),
                headerData.getBaselines(),
                signalData.getChannelOneAdc(),
                signalData.getChannelTwoAdc(),
                signalData.getChannelOneMv(),
                signalData.getChannelTwoMv(),
                annotations
        );

        recordRepository.saveOrUpdate(record);

        return record;
    }
}