package com.ecganalyzer.service;

import com.ecganalyzer.model.ECGRecord;

import java.io.File;

public interface ECGRecordService {

    ECGRecord loadRecord(File selectedFile);
}