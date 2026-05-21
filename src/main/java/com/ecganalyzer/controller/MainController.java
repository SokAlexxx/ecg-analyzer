package com.ecganalyzer.controller;

import com.ecganalyzer.config.AppConfig;
import com.ecganalyzer.database.DatabaseManager;
import com.ecganalyzer.model.ApplicationView;
import com.ecganalyzer.model.AppState;
import com.ecganalyzer.model.ECGRecord;
import com.ecganalyzer.model.ECGRecordSummary;
import com.ecganalyzer.repository.ECGRecordRepository;
import com.ecganalyzer.service.ECGRecordService;
import com.ecganalyzer.service.ECGRecordServiceImpl;
import com.ecganalyzer.util.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MainController {

    @FXML
    private Label titleLabel;

    @FXML
    private Label descriptionLabel;

    @FXML
    private Label statusLabel;

    @FXML
    private Label recordNameLabel;

    @FXML
    private Label channelsLabel;

    @FXML
    private Label frequencyLabel;

    @FXML
    private Label samplesLabel;

    @FXML
    private Label signalFormatLabel;

    @FXML
    private Label gainLabel;

    @FXML
    private Label baselineLabel;

    @FXML
    private Label signalFileLabel;

    @FXML
    private Label annotationFileLabel;

    @FXML
    private Label annotationsLabel;

    @FXML
    private ListView<String> recordsListView;

    private final AppState appState = new AppState();
    private final ECGRecordService ecgRecordService = new ECGRecordServiceImpl();
    private final ECGRecordRepository recordRepository = new ECGRecordRepository();

    private static final Logger logger =
            LoggerFactory.getLogger(MainController.class);

    @FXML
    private void initialize() {
        logger.info("Application initialized");

        DatabaseManager.initializeDatabase();

        switchView(appState.getCurrentView());
        loadSavedRecords();

        setStatus("Ready. Saved ECG records: " + recordRepository.countRecords());
    }

    @FXML
    private void onNewProject() {
        clearRecordInfo();
        setStatus("New project action selected");
    }

    @FXML
    private void onOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select MIT-BIH ECG Record");

        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter(
                        "MIT-BIH ECG Files",
                        "*.hea",
                        "*.dat",
                        "*.atr"
                )
        );

        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());

        if (selectedFile == null) {
            setStatus("ECG record loading cancelled");
            return;
        }

        try {
            ECGRecord record = ecgRecordService.loadRecord(selectedFile);
            showRecordInfo(record);
            loadSavedRecords();
            setStatus("ECG record loaded: " + record.getRecordName());
            logger.info("ECG record loaded: {}", record.getRecordName());
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage());
            logger.error("Failed to load ECG record", e);
        }
    }

    @FXML
    private void onSave() {
        setStatus("Current ECG record metadata is saved automatically");
    }

    @FXML
    private void onExit() {
        Platform.exit();
    }

    @FXML
    private void onZoomIn() {
        setStatus("Zoom in action selected");
    }

    @FXML
    private void onZoomOut() {
        setStatus("Zoom out action selected");
    }

    @FXML
    private void onResetView() {
        setStatus("Reset view action selected");
    }

    @FXML
    private void onDashboard() {
        switchView(ApplicationView.DASHBOARD);
    }

    @FXML
    private void onEcgRecords() {
        titleLabel.setText("ECG Records");
        descriptionLabel.setText("Module 2: ECG Record Management");
        loadSavedRecords();
        setStatus("ECG Records view selected");
    }

    @FXML
    private void onVisualization() {
        switchView(ApplicationView.VISUALIZATION);
    }

    @FXML
    private void onSettings() {
        switchView(ApplicationView.SETTINGS);
    }

    @FXML
    private void onAbout() {
        logger.info("Opening About dialog");

        DialogUtils.showInfo(
                "About",
                AppConfig.APPLICATION_TITLE,
                "Module 2: ECG Record Management"
        );

        setStatus("About dialog opened");
    }

    private void switchView(ApplicationView view) {
        logger.info("Switching to view: {}", view);

        appState.setCurrentView(view);

        titleLabel.setText(view.getTitle());
        descriptionLabel.setText(view.getDescription());

        setStatus(view.getTitle() + " view selected");
    }

    private void showRecordInfo(ECGRecord record) {
        recordNameLabel.setText("Record name: " + record.getRecordName());

        channelsLabel.setText("Channels: " + record.getChannels()
                + " " + record.getLeadNames());

        frequencyLabel.setText("Sampling frequency: "
                + record.getSamplingFrequency() + " Hz");

        samplesLabel.setText("Samples: " + record.getSampleCount());

        signalFormatLabel.setText("Signal format: " + record.getSignalFormats());

        gainLabel.setText("Gain: " + record.getGains());

        baselineLabel.setText("Baseline: " + record.getBaselines());

        signalFileLabel.setText("Signal file: "
                + record.getSignalFile().getName());

        if (record.getAnnotationFile() != null) {
            annotationFileLabel.setText("Annotation file: "
                    + record.getAnnotationFile().getName());
        } else {
            annotationFileLabel.setText("Annotation file: not found");
        }

        annotationsLabel.setText("Annotations: " + record.getAnnotations().size());

        setStatus("Loaded " + record.getChannelOneSignal().length
                + " ECG samples and "
                + record.getAnnotations().size()
                + " annotations");
    }

    private void loadSavedRecords() {
        List<ECGRecordSummary> summaries = recordRepository.findAllSummaries();

        List<String> items = summaries.stream()
                .map(this::formatSummary)
                .toList();

        recordsListView.setItems(FXCollections.observableArrayList(items));
    }

    private String formatSummary(ECGRecordSummary summary) {
        return summary.getRecordName()
                + " | "
                + summary.getSamplingFrequency()
                + " Hz | "
                + summary.getLeadNames()
                + " | annotations: "
                + summary.getAnnotationsCount();
    }

    private void clearRecordInfo() {
        recordNameLabel.setText("Record name: —");
        channelsLabel.setText("Channels: —");
        frequencyLabel.setText("Sampling frequency: —");
        samplesLabel.setText("Samples: —");
        signalFormatLabel.setText("Signal format: —");
        gainLabel.setText("Gain: —");
        baselineLabel.setText("Baseline: —");
        signalFileLabel.setText("Signal file: —");
        annotationFileLabel.setText("Annotation file: —");
        annotationsLabel.setText("Annotations: —");
    }

    private void setStatus(String message) {
        statusLabel.setText("Status: " + message);
    }
}