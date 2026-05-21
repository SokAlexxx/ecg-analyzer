package com.ecganalyzer.controller;

import com.ecganalyzer.config.AppConfig;
import com.ecganalyzer.database.DatabaseManager;
import com.ecganalyzer.model.AppState;
import com.ecganalyzer.model.ApplicationView;
import com.ecganalyzer.model.ECGRecord;
import com.ecganalyzer.model.ECGRecordSummary;
import com.ecganalyzer.model.SignalWindow;
import com.ecganalyzer.repository.ECGRecordRepository;
import com.ecganalyzer.service.ECGRecordService;
import com.ecganalyzer.service.ECGRecordServiceImpl;
import com.ecganalyzer.service.SignalVisualizationService;
import com.ecganalyzer.service.SignalWindowService;
import com.ecganalyzer.util.DialogUtils;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class MainController {

    private static final int DEFAULT_WINDOW_SIZE = 5000;
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

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
    private Label windowInfoLabel;

    @FXML
    private Label chartHintLabel;

    @FXML
    private ListView<String> recordsListView;

    @FXML
    private LineChart<Number, Number> signalChart;

    @FXML
    private NumberAxis xAxis;

    @FXML
    private NumberAxis yAxis;

    private final AppState appState = new AppState();
    private final ECGRecordService ecgRecordService = new ECGRecordServiceImpl();
    private final ECGRecordRepository recordRepository = new ECGRecordRepository();
    private final SignalWindowService signalWindowService = new SignalWindowService();
    private final SignalVisualizationService signalVisualizationService = new SignalVisualizationService();

    private ECGRecord currentRecord;
    private int currentStartIndex;
    private int currentWindowSize = DEFAULT_WINDOW_SIZE;
    private double dragStartX;

    @FXML
    private void initialize() {
        logger.info("Application initialized");

        DatabaseManager.initializeDatabase();

        configureChartInteraction();
        switchView(appState.getCurrentView());
        loadSavedRecords();
        updateWindowInfo(null);

        setStatus("Ready. Saved ECG records: " + recordRepository.countRecords());
    }

    @FXML
    private void onNewProject() {
        currentRecord = null;
        currentStartIndex = 0;
        currentWindowSize = DEFAULT_WINDOW_SIZE;

        signalChart.getData().clear();
        clearRecordInfo();
        updateWindowInfo(null);

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
            currentRecord = record;
            currentStartIndex = 0;
            currentWindowSize = Math.min(DEFAULT_WINDOW_SIZE, record.getChannelOneMv().length);

            showRecordInfo(record);
            loadSavedRecords();
            drawCurrentWindow();
            switchView(ApplicationView.VISUALIZATION);

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
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before zooming");
            return;
        }

        currentWindowSize = signalWindowService.zoomIn(currentWindowSize);
        drawCurrentWindow();
        setStatus("Signal zoomed in");
    }

    @FXML
    private void onZoomOut() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before zooming");
            return;
        }

        currentWindowSize = signalWindowService.zoomOut(currentRecord.getChannelOneMv(), currentWindowSize);
        drawCurrentWindow();
        setStatus("Signal zoomed out");
    }

    @FXML
    private void onResetView() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before resetting view");
            return;
        }

        currentStartIndex = 0;
        currentWindowSize = Math.min(DEFAULT_WINDOW_SIZE, currentRecord.getChannelOneMv().length);
        drawCurrentWindow();
        setStatus("Signal view reset");
    }

    @FXML
    private void onSignalStart() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before navigation");
            return;
        }

        currentStartIndex = 0;
        drawCurrentWindow();
        setStatus("Moved to signal start");
    }

    @FXML
    private void onSignalPrevious() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before navigation");
            return;
        }

        currentStartIndex = signalWindowService.moveLeft(currentStartIndex, currentWindowSize);
        drawCurrentWindow();
        setStatus("Moved to previous signal window");
    }

    @FXML
    private void onSignalNext() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before navigation");
            return;
        }

        currentStartIndex = signalWindowService.moveRight(
                currentRecord.getChannelOneMv(),
                currentStartIndex,
                currentWindowSize
        );
        drawCurrentWindow();
        setStatus("Moved to next signal window");
    }

    @FXML
    private void onSignalEnd() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before navigation");
            return;
        }

        currentStartIndex = Math.max(0, currentRecord.getChannelOneMv().length - currentWindowSize);
        drawCurrentWindow();
        setStatus("Moved to signal end");
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
        if (hasLoadedSignal()) {
            drawCurrentWindow();
        }
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
                "Module 3: ECG Signal Visualization"
        );

        setStatus("About dialog opened");
    }

    private void configureChartInteraction() {
        signalChart.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() > 0) {
                onZoomIn();
            } else {
                onZoomOut();
            }
            event.consume();
        });

        signalChart.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> dragStartX = event.getX());

        signalChart.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!hasLoadedSignal()) {
                return;
            }

            double deltaX = event.getX() - dragStartX;

            if (Math.abs(deltaX) < 30) {
                return;
            }

            int shift = Math.max(100, currentWindowSize / 5);

            if (deltaX < 0) {
                currentStartIndex = signalWindowService.moveRight(
                        currentRecord.getChannelOneMv(),
                        currentStartIndex,
                        shift
                );
            } else {
                currentStartIndex = signalWindowService.moveLeft(currentStartIndex, shift);
            }

            dragStartX = event.getX();
            drawCurrentWindow();
            event.consume();
        });
    }

    private void drawCurrentWindow() {
        if (!hasLoadedSignal()) {
            signalChart.getData().clear();
            updateWindowInfo(null);
            return;
        }

        SignalWindow window = signalWindowService.createWindow(
                currentRecord.getChannelOneMv(),
                currentStartIndex,
                currentWindowSize
        );

        currentStartIndex = window.getStartIndex();

        signalVisualizationService.drawSignal(
                signalChart,
                xAxis,
                yAxis,
                window,
                currentRecord.getSamplingFrequency()
        );

        updateWindowInfo(window);
    }

    private boolean hasLoadedSignal() {
        return currentRecord != null
                && currentRecord.getChannelOneMv() != null
                && currentRecord.getChannelOneMv().length > 0;
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
        signalFileLabel.setText("Signal file: " + record.getSignalFile().getName());

        if (record.getAnnotationFile() != null) {
            annotationFileLabel.setText("Annotation file: " + record.getAnnotationFile().getName());
        } else {
            annotationFileLabel.setText("Annotation file: not found");
        }

        annotationsLabel.setText("Annotations: " + record.getAnnotations().size());

        setStatus("Loaded " + record.getChannelOneSignal().length
                + " ECG samples and "
                + record.getAnnotations().size()
                + " annotations");
    }

    private void updateWindowInfo(SignalWindow window) {
        if (window == null || !hasLoadedSignal()) {
            windowInfoLabel.setText("Window: —");
            return;
        }

        double startTime = (double) window.getStartIndex() / currentRecord.getSamplingFrequency();
        double endTime = (double) window.getEndIndex() / currentRecord.getSamplingFrequency();

        windowInfoLabel.setText(
                "Window: samples "
                        + window.getStartIndex()
                        + "–"
                        + window.getEndIndex()
                        + " | time "
                        + String.format("%.2f", startTime)
                        + "–"
                        + String.format("%.2f", endTime)
                        + " s | size "
                        + window.size()
        );
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
