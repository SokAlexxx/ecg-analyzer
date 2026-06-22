package com.ecganalyzer.controller;

import com.ecganalyzer.config.AppConfig;
import com.ecganalyzer.database.DatabaseManager;
import com.ecganalyzer.model.AnnotationTableRow;
import com.ecganalyzer.model.ECGAnnotation;
import com.ecganalyzer.model.ECGAnalysisResult;
import com.ecganalyzer.model.ECGRecord;
import com.ecganalyzer.model.RPeak;
import com.ecganalyzer.model.SignalWindow;
import com.ecganalyzer.model.SuspiciousSegment;
import com.ecganalyzer.repository.ECGRecordRepository;
import com.ecganalyzer.service.ECGRecordService;
import com.ecganalyzer.service.ECGRecordServiceImpl;
import com.ecganalyzer.service.PdfReportService;
import com.ecganalyzer.service.SignalAnalysisService;
import com.ecganalyzer.service.SignalVisualizationService;
import com.ecganalyzer.service.SignalWindowService;
import com.ecganalyzer.settings.AppSettings;
import com.ecganalyzer.settings.AppSettingsService;
import com.ecganalyzer.util.DialogUtils;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Slider;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.WritableImage;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MainController {

    private static final int DEFAULT_WINDOW_SIZE = 5000;
    private static final int MIN_WINDOW_SIZE = 500;
    private static final int MAX_WINDOW_SIZE = 30000;
    private static final double MIN_Y_SCALE_FACTOR = 0.2;
    private static final double MAX_Y_SCALE_FACTOR = 10.0;
    private static final int MAX_NAVIGATION_HISTORY = 80;
    private static final Logger logger = LoggerFactory.getLogger(MainController.class);

    @FXML private Label titleLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label statusLabel;
    @FXML private Label recordNameLabel;
    @FXML private Label channelsLabel;
    @FXML private Label frequencyLabel;
    @FXML private Label samplesLabel;
    @FXML private Label signalFormatLabel;
    @FXML private Label gainLabel;
    @FXML private Label baselineLabel;
    @FXML private Label signalFileLabel;
    @FXML private Label annotationFileLabel;
    @FXML private Label annotationsLabel;
    @FXML private Label durationLabel;
    @FXML private Label windowInfoLabel;
    @FXML private Label scaleInfoLabel;
    @FXML private Label segmentInfoLabel;
    @FXML private Label selectedEventLabel;
    @FXML private Label chartHintLabel;
    @FXML private Label annotationStatsLabel;
    @FXML private Label analysisStatsLabel;
    @FXML private Label heartRateLabel;
    @FXML private Label variabilityLabel;
    @FXML private Label suspiciousSegmentsLabel;
    @FXML private Label detectionAccuracyLabel;
    @FXML private Label rhythmStatusLabel;
    @FXML private Label contextAnalysisLabel;

    @FXML private BorderPane rootPane;
    @FXML private Slider navigationSlider;
    @FXML private ListView<String> recordsListView;
    @FXML private LineChart<Number, Number> signalChart;
    @FXML private NumberAxis xAxis;
    @FXML private NumberAxis yAxis;
    @FXML private CheckBox showRPeaksCheckBox;
    @FXML private CheckBox showAnnotationsCheckBox;
    @FXML private CheckBox showSuspiciousSegmentsCheckBox;
    @FXML private CheckBox showGridCheckBox;
    @FXML private ComboBox<String> annotationFilterComboBox;
    @FXML private ComboBox<String> analysisModeComboBox;
    @FXML private ComboBox<String> themeComboBox;
    @FXML private TextField annotationSearchField;
    @FXML private TableView<AnnotationTableRow> annotationsTableView;
    @FXML private TableColumn<AnnotationTableRow, Long> sampleColumn;
    @FXML private TableColumn<AnnotationTableRow, String> timeColumn;
    @FXML private TableColumn<AnnotationTableRow, String> typeColumn;
    @FXML private TableColumn<AnnotationTableRow, String> sourceColumn;
    @FXML private TableColumn<AnnotationTableRow, String> descriptionColumn;

    private final ECGRecordService ecgRecordService = new ECGRecordServiceImpl();
    private final ECGRecordRepository recordRepository = new ECGRecordRepository();
    private final SignalWindowService signalWindowService = new SignalWindowService();
    private final SignalVisualizationService signalVisualizationService = new SignalVisualizationService();
    private final SignalAnalysisService signalAnalysisService = new SignalAnalysisService();
    private final PdfReportService pdfReportService = new PdfReportService();
    private final AppSettingsService appSettingsService = new AppSettingsService();
    private AppSettings appSettings = appSettingsService.load();

    private ECGRecord currentRecord;
    private ECGAnalysisResult currentAnalysisResult;
    private int currentStartIndex;
    private int currentWindowSize = DEFAULT_WINDOW_SIZE;
    private int segmentStartIndex = -1;
    private int segmentEndIndex = -1;
    private int lastContextSampleIndex = -1;
    private double dragStartX;
    private double xAxisDragStartX;
    private int xAxisDragStartWindowSize;
    private int xAxisDragCenterIndex;
    private double yAxisDragStartY;
    private double yAxisDragStartScaleFactor = 1.0;
    private double yScaleFactor = 1.0;
    private long selectedAnnotationSampleIndex = -1;
    private boolean updatingNavigationSlider;
    private int analysisStartIndex = -1;
    private int analysisEndIndex = -1;
    private int suspiciousSegmentCursor = -1;
    private RPeakDetectionMetrics currentDetectionMetrics = RPeakDetectionMetrics.unavailable();
    private String currentAnalysisScope = "—";
    private final Deque<UndoSnapshot> undoStack = new ArrayDeque<>();
    private final Deque<ChartState> chartUndoStack = new ArrayDeque<>();
    private final Deque<ChartState> chartRedoStack = new ArrayDeque<>();
    private boolean restoringChartState;

    @FXML
    private void initialize() {
        logger.info("Application initialized");

        DatabaseManager.initializeDatabase();
        configureChart();
        configureAnnotationTable();
        configureAnalysisControls();
        configureNavigationSlider();
        configureChartInteraction();
        configureLayerControls();
        configureThemeControls();
        applySavedSettingsToControls();
        Platform.runLater(this::configureKeyboardShortcuts);
        clearRecordInfo();
        clearAnalysis();
        clearAnnotations();
        updateWindowInfo(null);
        updateSegmentInfo();
        loadSavedRecords();

        setStatus("Ready. Saved ECG records: " + recordRepository.countRecords());
    }

    @FXML
    private void onOpen() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select MIT-BIH ECG Record");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("MIT-BIH ECG Files", "*.hea", "*.dat", "*.atr"));

        File selectedFile = fileChooser.showOpenDialog(statusLabel.getScene().getWindow());
        if (selectedFile == null) {
            setStatus("ECG record loading cancelled");
            return;
        }

        try {
            ECGRecord record = ecgRecordService.loadRecord(selectedFile);
            currentRecord = record;
            currentAnalysisResult = null;
            currentStartIndex = 0;
            currentWindowSize = Math.min(getDefaultWindowSize(), record.getChannelOneMv().length);
            yScaleFactor = 1.0;
            segmentStartIndex = -1;
            segmentEndIndex = -1;
            selectedAnnotationSampleIndex = -1;
            lastContextSampleIndex = -1;
            resetAnalysisNavigation();
            undoStack.clear();
            chartUndoStack.clear();
            chartRedoStack.clear();

            if (showAnnotationsCheckBox != null) {
                showAnnotationsCheckBox.setSelected(appSettings.isShowAnnotationsByDefault());
            }
            if (showSuspiciousSegmentsCheckBox != null) {
                showSuspiciousSegmentsCheckBox.setSelected(appSettings.isShowSuspiciousSegmentsByDefault());
            }
            if (showRPeaksCheckBox != null) {
                showRPeaksCheckBox.setSelected(false);
            }

            showRecordInfo(record);
            loadAnnotations(record);
            clearAnalysis();
            drawCurrentWindow();
            loadSavedRecords();
            setStatus("ECG record loaded: " + record.getRecordName());
        } catch (Exception e) {
            setStatus("Error: " + e.getMessage());
            logger.error("Failed to load ECG record", e);
        }
    }

    @FXML private void onSave() { setStatus("Current ECG record metadata is saved automatically"); }
    @FXML private void onExit() { Platform.exit(); }

    @FXML
    private void onNewProject() {
        currentRecord = null;
        currentAnalysisResult = null;
        currentStartIndex = 0;
        currentWindowSize = getDefaultWindowSize();
        yScaleFactor = 1.0;
        segmentStartIndex = -1;
        segmentEndIndex = -1;
        selectedAnnotationSampleIndex = -1;
        undoStack.clear();
        chartUndoStack.clear();
        chartRedoStack.clear();
        signalChart.getData().clear();
        clearRecordInfo();
        clearAnnotations();
        clearAnalysis();
        updateWindowInfo(null);
        updateNavigationSlider(null);
        updateSegmentInfo();
        setStatus("New workspace created");
    }

    @FXML
    private void onZoomIn() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before zooming");
            return;
        }
        saveChartStateForUndo();
        currentWindowSize = Math.max(MIN_WINDOW_SIZE, signalWindowService.zoomIn(currentWindowSize));
        currentStartIndex = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), currentStartIndex, currentWindowSize);
        drawCurrentWindow();
        setStatus("Signal zoomed in");
    }

    @FXML
    private void onZoomOut() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before zooming");
            return;
        }
        saveChartStateForUndo();
        currentWindowSize = Math.min(MAX_WINDOW_SIZE, signalWindowService.zoomOut(currentRecord.getChannelOneMv(), currentWindowSize));
        currentStartIndex = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), currentStartIndex, currentWindowSize);
        drawCurrentWindow();
        setStatus("Signal zoomed out");
    }

    @FXML
    private void onResetView() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before resetting view");
            return;
        }
        saveChartStateForUndo();
        currentStartIndex = 0;
        currentWindowSize = Math.min(getDefaultWindowSize(), currentRecord.getChannelOneMv().length);
        yScaleFactor = 1.0;
        drawCurrentWindow();
        setStatus("Signal view reset");
    }

    @FXML
    private void onFitSignal() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before fitting signal");
            return;
        }
        saveChartStateForUndo();
        currentStartIndex = 0;
        currentWindowSize = Math.min(MAX_WINDOW_SIZE, currentRecord.getChannelOneMv().length);
        yScaleFactor = 1.0;
        drawCurrentWindow();
        setStatus("Signal fitted to available chart range");
    }

    @FXML
    private void onRunAnalysis() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before signal analysis");
            return;
        }

        String mode = analysisModeComboBox == null ? "Full signal" : analysisModeComboBox.getSelectionModel().getSelectedItem();
        if (mode == null || mode.equals("Full signal")) {
            onAnalyzeSignal();
        } else if (mode.equals("Visible window")) {
            onAnalyzeVisibleSegment();
        } else if (mode.equals("Selected segment")) {
            onAnalyzeSelectedSegment();
        }
    }

    @FXML
    private void onGoToSuspiciousSegment() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before navigation");
            return;
        }
        if (currentAnalysisResult == null || currentAnalysisResult.getSuspiciousSegments().isEmpty()) {
            setStatus("No suspicious RR segments. Run analysis first");
            return;
        }

        List<SuspiciousSegment> availableSegments = currentAnalysisResult.getSuspiciousSegments().stream()
                .filter(this::isSuspiciousSegmentInsideAnalysisBounds)
                .sorted(Comparator.comparingInt(SuspiciousSegment::getStartSampleIndex))
                .toList();
        if (availableSegments.isEmpty()) {
            setStatus("No suspicious RR segments inside selected analysis bounds");
            return;
        }

        if (suspiciousSegmentCursor >= availableSegments.size() - 1) {
            suspiciousSegmentCursor = -1;
        }
        suspiciousSegmentCursor = (suspiciousSegmentCursor + 1) % availableSegments.size();
        SuspiciousSegment segment = availableSegments.get(suspiciousSegmentCursor);
        int center = (segment.getStartSampleIndex() + segment.getEndSampleIndex()) / 2;

        if (showSuspiciousSegmentsCheckBox != null) {
            showSuspiciousSegmentsCheckBox.setSelected(true);
        }
        saveChartStateForUndo();
        focusSuspiciousSegment(segment, center);
        drawCurrentWindow();
        setStatus("Suspicious segment " + (suspiciousSegmentCursor + 1) + "/" + availableSegments.size()
                + ": " + segment.getType() + " " + formatTime(segment.getStartSampleIndex())
                + "–" + formatTime(segment.getEndSampleIndex()));
    }

    @FXML
    private void onShowAnalysisReport() {
        if (currentAnalysisResult == null) {
            setStatus("Run analysis before opening report");
            return;
        }
        DialogUtils.showInfo("Analysis report", "ECG analysis report", buildAnalysisReport());
        setStatus("Analysis report opened");
    }

    @FXML
    private void onExportAnalysisReport() {
        if (currentAnalysisResult == null) {
            setStatus("Run analysis before exporting report");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export analysis report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text file", "*.txt"));
        fileChooser.setInitialFileName(currentRecord == null ? "analysis-report.txt" : currentRecord.getRecordName() + "-analysis-report.txt");
        File outputFile = fileChooser.showSaveDialog(signalChart.getScene().getWindow());
        if (outputFile == null) {
            setStatus("Analysis report export cancelled");
            return;
        }

        try {
            Files.writeString(outputFile.toPath(), buildAnalysisReport(), StandardCharsets.UTF_8);
            setStatus("Analysis report exported: " + outputFile.getName());
        } catch (IOException exception) {
            logger.error("Failed to export analysis report", exception);
            DialogUtils.showError("Export error", "Unable to export analysis report", exception.getMessage());
            setStatus("Analysis report export failed");
        }
    }


    @FXML
    private void onExportPdfReport() {
        if (currentAnalysisResult == null) {
            setStatus("Run analysis before PDF report export");
            return;
        }
        if (signalChart == null) {
            setStatus("Chart is unavailable for PDF report export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export PDF analysis report");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF document", "*.pdf"));
        fileChooser.setInitialFileName(currentRecord == null ? "ecg-analysis-report.pdf" : currentRecord.getRecordName() + "-analysis-report.pdf");
        File outputFile = fileChooser.showSaveDialog(signalChart.getScene().getWindow());
        if (outputFile == null) {
            setStatus("PDF report export cancelled");
            return;
        }

        if (!outputFile.getName().toLowerCase().endsWith(".pdf")) {
            outputFile = new File(outputFile.getParentFile(), outputFile.getName() + ".pdf");
        }

        try {
            WritableImage chartImage = signalChart.snapshot(null, null);
            pdfReportService.exportAnalysisReport(outputFile, buildAnalysisReport(), chartImage);
            setStatus("PDF report exported: " + outputFile.getName());
        } catch (IOException exception) {
            logger.error("Failed to export PDF report", exception);
            DialogUtils.showError("Export error", "Unable to export PDF report", exception.getMessage());
            setStatus("PDF report export failed");
        }
    }

    @FXML
    private void onExportChartImage() {
        if (!hasLoadedSignal() || signalChart == null) {
            setStatus("Load ECG record before chart export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export ECG chart image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PNG image", "*.png"));
        fileChooser.setInitialFileName(currentRecord == null ? "ecg-chart.png" : currentRecord.getRecordName() + "-chart.png");
        File outputFile = fileChooser.showSaveDialog(signalChart.getScene().getWindow());
        if (outputFile == null) {
            setStatus("Chart image export cancelled");
            return;
        }

        try {
            WritableImage image = signalChart.snapshot(null, null);
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", outputFile);
            setStatus("Chart image exported: " + outputFile.getName());
        } catch (IOException exception) {
            logger.error("Failed to export chart image", exception);
            DialogUtils.showError("Export error", "Unable to export chart image", exception.getMessage());
            setStatus("Chart image export failed");
        }
    }

    @FXML
    private void onExportAnalysisSummaryCsv() {
        if (currentAnalysisResult == null) {
            setStatus("Run analysis before metrics export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export analysis metrics");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        fileChooser.setInitialFileName(currentRecord == null ? "analysis-metrics.csv" : currentRecord.getRecordName() + "-analysis-metrics.csv");
        File outputFile = fileChooser.showSaveDialog(signalChart.getScene().getWindow());
        if (outputFile == null) {
            setStatus("Analysis metrics export cancelled");
            return;
        }

        try {
            Files.writeString(outputFile.toPath(), buildAnalysisSummaryCsv(), StandardCharsets.UTF_8);
            setStatus("Analysis metrics exported: " + outputFile.getName());
        } catch (IOException exception) {
            logger.error("Failed to export analysis metrics", exception);
            DialogUtils.showError("Export error", "Unable to export analysis metrics", exception.getMessage());
            setStatus("Analysis metrics export failed");
        }
    }

    @FXML
    private void onAnalyzeSignal() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before signal analysis");
            return;
        }
        double[] source = currentRecord.getChannelOneMv();
        currentAnalysisResult = annotateSuspiciousSegments(
                signalAnalysisService.analyze(source, currentRecord.getSamplingFrequency())
        );
        analysisStartIndex = 0;
        analysisEndIndex = source.length - 1;
        suspiciousSegmentCursor = -1;
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.setSelected(true);
        }
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.setSelected(appSettings.isShowRPeaksByDefault() || showRPeaksCheckBox.isSelected());
        }
        if (showSuspiciousSegmentsCheckBox != null) {
            showSuspiciousSegmentsCheckBox.setSelected(!currentAnalysisResult.getSuspiciousSegments().isEmpty());
        }
        showAnalysisResult(currentAnalysisResult, "Full signal");
        drawCurrentWindow();
        setStatus("Full signal analysis completed");
    }

    @FXML
    private void onAnalyzeVisibleSegment() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before segment analysis");
            return;
        }
        analyzeRange(currentStartIndex, Math.min(currentRecord.getChannelOneMv().length - 1, currentStartIndex + currentWindowSize - 1), "Visible segment");
    }

    @FXML
    private void onAnalyzeSelectedSegment() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before segment analysis");
            return;
        }
        if (!hasValidSegment()) {
            setStatus("Select segment start and end before segment analysis");
            return;
        }
        analyzeRange(getSegmentFrom(), getSegmentTo(), "Selected segment");
    }

    @FXML
    private void onSetSegmentStart() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before selecting segment");
            return;
        }
        int sample = lastContextSampleIndex >= 0 ? lastContextSampleIndex : currentStartIndex;
        setSegmentStart(sample);
    }

    @FXML
    private void onSetSegmentEnd() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before selecting segment");
            return;
        }
        int fallback = Math.min(currentRecord.getChannelOneMv().length - 1, currentStartIndex + currentWindowSize - 1);
        int sample = lastContextSampleIndex >= 0 ? lastContextSampleIndex : fallback;
        setSegmentEnd(sample);
    }

    @FXML
    private void onClearSegmentSelection() {
        if (hasLoadedSignal()) { saveChartStateForUndo(); }
        segmentStartIndex = -1;
        segmentEndIndex = -1;
        updateSegmentInfo();
        drawCurrentWindow();
        setStatus("Segment selection cleared");
    }

    @FXML private void onSignalStart() { moveToStart(0, "Moved to signal start"); }
    @FXML private void onSignalPrevious() { moveToStart(signalWindowService.moveLeft(currentStartIndex, currentWindowSize), "Moved to previous signal window"); }
    @FXML private void onSignalNext() { moveToStart(signalWindowService.moveRight(currentRecord == null ? null : currentRecord.getChannelOneMv(), currentStartIndex, currentWindowSize), "Moved to next signal window"); }
    @FXML private void onSignalEnd() {
        if (!hasLoadedSignal()) { setStatus("Load ECG record before navigation"); return; }
        moveToStart(Math.max(0, currentRecord.getChannelOneMv().length - currentWindowSize), "Moved to signal end");
    }

    @FXML private void onDashboard() { setStatus("Workspace view selected"); }
    @FXML private void onEcgRecords() { setStatus("Records are available through Load ECG Record"); }
    @FXML private void onVisualization() { drawCurrentWindow(); }
    @FXML
    private void onSettings() {
        Dialog<AppSettings> dialog = new Dialog<>();
        dialog.setTitle("Settings");
        dialog.setHeaderText("Application settings");

        ButtonType saveButtonType = new ButtonType("Save", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        Spinner<Integer> windowSizeSpinner = new Spinner<>();
        windowSizeSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(
                MIN_WINDOW_SIZE,
                MAX_WINDOW_SIZE,
                clamp(appSettings.getDefaultWindowSize(), MIN_WINDOW_SIZE, MAX_WINDOW_SIZE),
                500
        ));
        windowSizeSpinner.setEditable(true);

        ComboBox<String> defaultAnalysisModeComboBox = new ComboBox<>();
        defaultAnalysisModeComboBox.setItems(FXCollections.observableArrayList("Full signal", "Visible window", "Selected segment"));
        defaultAnalysisModeComboBox.getSelectionModel().select(normalizeAnalysisMode(appSettings.getDefaultAnalysisMode()));

        CheckBox gridCheckBox = new CheckBox("Show grid by default");
        gridCheckBox.setSelected(appSettings.isShowGridByDefault());

        CheckBox annotationsCheckBox = new CheckBox("Show annotations after loading record");
        annotationsCheckBox.setSelected(appSettings.isShowAnnotationsByDefault());

        CheckBox rPeaksCheckBox = new CheckBox("Show R-peaks after analysis");
        rPeaksCheckBox.setSelected(appSettings.isShowRPeaksByDefault());

        CheckBox suspiciousCheckBox = new CheckBox("Show suspicious segments after analysis");
        suspiciousCheckBox.setSelected(appSettings.isShowSuspiciousSegmentsByDefault());

        GridPane gridPane = new GridPane();
        gridPane.setHgap(12);
        gridPane.setVgap(10);
        gridPane.add(new Label("Default window size, samples:"), 0, 0);
        gridPane.add(windowSizeSpinner, 1, 0);
        gridPane.add(new Label("Default analysis mode:"), 0, 1);
        gridPane.add(defaultAnalysisModeComboBox, 1, 1);
        gridPane.add(gridCheckBox, 0, 2, 2, 1);
        gridPane.add(annotationsCheckBox, 0, 3, 2, 1);
        gridPane.add(rPeaksCheckBox, 0, 4, 2, 1);
        gridPane.add(suspiciousCheckBox, 0, 5, 2, 1);

        dialog.getDialogPane().setContent(gridPane);
        dialog.setResultConverter(buttonType -> {
            if (buttonType != saveButtonType) {
                return null;
            }
            AppSettings updatedSettings = new AppSettings();
            updatedSettings.setDefaultWindowSize(windowSizeSpinner.getValue());
            updatedSettings.setDefaultAnalysisMode(defaultAnalysisModeComboBox.getSelectionModel().getSelectedItem());
            updatedSettings.setShowGridByDefault(gridCheckBox.isSelected());
            updatedSettings.setShowAnnotationsByDefault(annotationsCheckBox.isSelected());
            updatedSettings.setShowRPeaksByDefault(rPeaksCheckBox.isSelected());
            updatedSettings.setShowSuspiciousSegmentsByDefault(suspiciousCheckBox.isSelected());
            return updatedSettings;
        });

        Optional<AppSettings> result = dialog.showAndWait();
        if (result.isEmpty()) {
            setStatus("Settings update cancelled");
            return;
        }

        try {
            appSettings = result.get();
            appSettingsService.save(appSettings);
            applySavedSettingsToControls();
            if (hasLoadedSignal()) {
                currentWindowSize = Math.min(getDefaultWindowSize(), currentRecord.getChannelOneMv().length);
                currentStartIndex = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), currentStartIndex, currentWindowSize);
                drawCurrentWindow();
            }
            setStatus("Settings saved");
        } catch (IOException exception) {
            logger.error("Failed to save application settings", exception);
            DialogUtils.showError("Settings error", "Unable to save settings", exception.getMessage());
            setStatus("Settings save failed");
        }
    }

    @FXML
    private void onPreviousAnnotation() {
        navigateAnnotation(-1);
    }

    @FXML
    private void onNextAnnotation() {
        navigateAnnotation(1);
    }

    @FXML
    private void onClearAnnotationSearch() {
        if (annotationSearchField != null) {
            annotationSearchField.clear();
            setStatus("Annotation search cleared");
        }
    }

    @FXML
    private void onExportAnnotationsCsv() {
        if (currentRecord == null || currentRecord.getAnnotations() == null || currentRecord.getAnnotations().isEmpty()) {
            setStatus("No annotations available for export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export annotations");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        fileChooser.setInitialFileName(currentRecord.getRecordName() + "-annotations.csv");
        File outputFile = fileChooser.showSaveDialog(signalChart.getScene().getWindow());
        if (outputFile == null) {
            setStatus("Annotation export cancelled");
            return;
        }

        try {
            Files.writeString(outputFile.toPath(), buildAnnotationsCsv(), StandardCharsets.UTF_8);
            setStatus("Annotations exported: " + outputFile.getName());
        } catch (IOException exception) {
            logger.error("Failed to export annotations", exception);
            DialogUtils.showError("Export error", "Unable to export annotations", exception.getMessage());
            setStatus("Annotation export failed");
        }
    }

    @FXML
    private void onExportRrIntervalsCsv() {
        if (currentAnalysisResult == null || currentAnalysisResult.getRrIntervalsSeconds().isEmpty()) {
            setStatus("Run analysis before RR interval export");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Export RR intervals");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV file", "*.csv"));
        fileChooser.setInitialFileName(currentRecord == null ? "rr-intervals.csv" : currentRecord.getRecordName() + "-rr-intervals.csv");
        File outputFile = fileChooser.showSaveDialog(signalChart.getScene().getWindow());
        if (outputFile == null) {
            setStatus("RR interval export cancelled");
            return;
        }

        try {
            Files.writeString(outputFile.toPath(), buildRrIntervalsCsv(), StandardCharsets.UTF_8);
            setStatus("RR intervals exported: " + outputFile.getName());
        } catch (IOException exception) {
            logger.error("Failed to export RR intervals", exception);
            DialogUtils.showError("Export error", "Unable to export RR intervals", exception.getMessage());
            setStatus("RR interval export failed");
        }
    }

    @FXML
    private void onAbout() {
        DialogUtils.showInfo("About", AppConfig.APPLICATION_TITLE, "Interactive ECG learning analyzer on Java");
        setStatus("About dialog opened");
    }

    @FXML
    private void onUndoLastAction() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before undo");
            return;
        }
        if (undoStack.isEmpty()) {
            setStatus("No manual changes to undo");
            return;
        }

        UndoSnapshot snapshot = undoStack.pop();
        currentRecord.getAnnotations().clear();
        currentRecord.getAnnotations().addAll(snapshot.annotations());
        refreshAnnotationFilterOptions(currentRecord.getAnnotations());
        refreshAnnotationTable();

        if (snapshot.peaks().isEmpty()) {
            currentAnalysisResult = null;
            clearAnalysis();
        } else {
            currentAnalysisResult = signalAnalysisService.analyzeWithRPeaks(
                    currentRecord.getChannelOneMv(),
                    currentRecord.getSamplingFrequency(),
                    snapshot.peaks()
            );
            showAnalysisResult(currentAnalysisResult, "Restored manual R-peak set");
        }

        drawCurrentWindow();
        setStatus("Last manual change undone");
    }

    @FXML
    private void onInteractionHelp() {
        DialogUtils.showInfo(
                "ECG interaction help",
                "Interactive controls",
                "Mouse wheel: zoom signal.\n"
                        + "Mouse drag: move along signal.\n"
                        + "Right click on graph: add/remove R-peak, add annotation, select or clear segment.\n"
                        + "Segment markers are shown immediately after setting start or end.\n"
                        + "User annotations are marked separately from MIT-BIH annotations.\n"
                        + "Ctrl+Z restores the previous chart/navigation state.\n"
                        + "Ctrl+Y reapplies the undone chart/navigation state.\n"
                        + "Undo manual edit restores the previous manual annotation/R-peak state."
        );
        setStatus("Interaction help opened");
    }

    private void configureThemeControls() {
        if (themeComboBox == null) {
            return;
        }

        themeComboBox.setItems(FXCollections.observableArrayList("Dark", "Light"));
        themeComboBox.getSelectionModel().select("Dark");
        themeComboBox.valueProperty().addListener((observable, oldValue, newValue) -> applyTheme(newValue));
        applyTheme(themeComboBox.getSelectionModel().getSelectedItem());
    }

    private void applyTheme(String themeName) {
        if (rootPane == null) {
            return;
        }

        rootPane.getStyleClass().remove("light-theme");
        if ("Light".equals(themeName)) {
            rootPane.getStyleClass().add("light-theme");
            setStatus("Light theme enabled");
        } else {
            setStatus("Dark theme enabled");
        }
    }


    private void applySavedSettingsToControls() {
        if (appSettings == null) {
            appSettings = new AppSettings();
        }
        appSettings.setDefaultWindowSize(clamp(appSettings.getDefaultWindowSize(), MIN_WINDOW_SIZE, MAX_WINDOW_SIZE));

        if (analysisModeComboBox != null) {
            analysisModeComboBox.getSelectionModel().select(normalizeAnalysisMode(appSettings.getDefaultAnalysisMode()));
        }
        if (showGridCheckBox != null) {
            showGridCheckBox.setSelected(appSettings.isShowGridByDefault());
            signalChart.setHorizontalGridLinesVisible(showGridCheckBox.isSelected());
            signalChart.setVerticalGridLinesVisible(showGridCheckBox.isSelected());
        }
        if (showAnnotationsCheckBox != null && currentRecord == null) {
            showAnnotationsCheckBox.setSelected(appSettings.isShowAnnotationsByDefault());
        }
        if (showRPeaksCheckBox != null && currentAnalysisResult == null) {
            showRPeaksCheckBox.setSelected(appSettings.isShowRPeaksByDefault());
        }
        if (showSuspiciousSegmentsCheckBox != null && currentAnalysisResult == null) {
            showSuspiciousSegmentsCheckBox.setSelected(appSettings.isShowSuspiciousSegmentsByDefault());
        }
    }

    private int getDefaultWindowSize() {
        if (appSettings == null) {
            return DEFAULT_WINDOW_SIZE;
        }
        return clamp(appSettings.getDefaultWindowSize(), MIN_WINDOW_SIZE, MAX_WINDOW_SIZE);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private String normalizeAnalysisMode(String value) {
        if (value == null) {
            return "Full signal";
        }
        return switch (value) {
            case "Visible window" -> "Visible window";
            case "Selected segment" -> "Selected segment";
            default -> "Full signal";
        };
    }

    private void configureChart() {
        signalChart.setAnimated(false);
        signalChart.setCreateSymbols(false);
        signalChart.setLegendVisible(false);
        xAxis.setAnimated(false);
        yAxis.setAnimated(false);
        xAxis.setForceZeroInRange(false);
        yAxis.setForceZeroInRange(false);
    }

    private void configureAnalysisControls() {
        if (analysisModeComboBox == null) {
            return;
        }
        analysisModeComboBox.setItems(FXCollections.observableArrayList(
                "Full signal",
                "Visible window",
                "Selected segment"
        ));
        analysisModeComboBox.getSelectionModel().select(normalizeAnalysisMode(appSettings.getDefaultAnalysisMode()));
    }

    private void configureLayerControls() {
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> drawCurrentWindow());
        }
        if (showAnnotationsCheckBox != null) {
            showAnnotationsCheckBox.setSelected(appSettings.isShowAnnotationsByDefault());
            showAnnotationsCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> drawCurrentWindow());
        }
        if (showSuspiciousSegmentsCheckBox != null) {
            showSuspiciousSegmentsCheckBox.setSelected(appSettings.isShowSuspiciousSegmentsByDefault());
            showSuspiciousSegmentsCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> drawCurrentWindow());
        }
        if (showGridCheckBox != null) {
            showGridCheckBox.selectedProperty().addListener((observable, oldValue, selected) -> {
                signalChart.setHorizontalGridLinesVisible(selected);
                signalChart.setVerticalGridLinesVisible(selected);
            });
            signalChart.setHorizontalGridLinesVisible(showGridCheckBox.isSelected());
            signalChart.setVerticalGridLinesVisible(showGridCheckBox.isSelected());
        }
    }

    private void configureAnnotationTable() {
        sampleColumn.setCellValueFactory(new PropertyValueFactory<>("sampleIndex"));
        timeColumn.setCellValueFactory(new PropertyValueFactory<>("timeSeconds"));
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        if (sourceColumn != null) {
            sourceColumn.setCellValueFactory(new PropertyValueFactory<>("source"));
        }
        descriptionColumn.setCellValueFactory(new PropertyValueFactory<>("description"));

        annotationFilterComboBox.setItems(FXCollections.observableArrayList("All"));
        annotationFilterComboBox.getSelectionModel().select("All");
        annotationFilterComboBox.setOnAction(event -> {
            refreshAnnotationTable();
            drawCurrentWindow();
        });

        if (annotationSearchField != null) {
            annotationSearchField.textProperty().addListener((observable, oldValue, newValue) -> {
                refreshAnnotationTable();
                drawCurrentWindow();
            });
        }

        annotationsTableView.setRowFactory(tableView -> new TableRow<>() {
            @Override
            protected void updateItem(AnnotationTableRow item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setStyle("");
                } else if (item.getSampleIndex() == selectedAnnotationSampleIndex) {
                    setStyle("-fx-background-color: #fff3cd;");
                } else if ("User".equalsIgnoreCase(item.getSource())) {
                    setStyle("-fx-background-color: #fff7e6;");
                } else {
                    setStyle("");
                }
            }
        });

        annotationsTableView.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, selectedRow) -> {
            if (selectedRow != null && hasLoadedSignal()) {
                selectedAnnotationSampleIndex = selectedRow.getSampleIndex();
                centerSignalOnSample(selectedRow.getSampleIndex());
                updateSelectedEvent("Annotation", (int) selectedRow.getSampleIndex(), selectedRow.getType(), selectedRow.getDescription());
                annotationsTableView.refresh();
            }
        });
    }

    private void configureKeyboardShortcuts() {
        if (signalChart == null || signalChart.getScene() == null) {
            return;
        }
        signalChart.getScene().addEventFilter(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            if (!event.isControlDown()) {
                return;
            }
            if (event.getCode() == KeyCode.Z) {
                undoChartAction();
                event.consume();
            } else if (event.getCode() == KeyCode.Y) {
                redoChartAction();
                event.consume();
            }
        });
    }

    private void configureNavigationSlider() {
        navigationSlider.setDisable(true);
        navigationSlider.valueChangingProperty().addListener((observable, wasChanging, isChanging) -> {
            if (!isChanging) {
                moveToSliderPosition();
            }
        });
        navigationSlider.setOnMouseReleased(event -> moveToSliderPosition());
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

        signalChart.addEventFilter(MouseEvent.MOUSE_MOVED, event -> {
            if (!hasLoadedSignal()) {
                return;
            }
            int sampleIndex = sampleFromChartMouseX(event.getX());
            if (sampleIndex >= 0 && sampleIndex < currentRecord.getChannelOneMv().length) {
                lastContextSampleIndex = sampleIndex;
                double timeSeconds = (double) sampleIndex / currentRecord.getSamplingFrequency();
                chartHintLabel.setText("Cursor: sample " + sampleIndex
                        + " | time " + String.format("%.2f", timeSeconds)
                        + " s | amplitude " + String.format("%.3f", currentRecord.getChannelOneMv()[sampleIndex])
                        + " mV");
            }
        });

        signalChart.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (event.getButton() == MouseButton.SECONDARY && hasLoadedSignal()) {
                int sample = sampleFromChartMouseX(event.getX());
                lastContextSampleIndex = sample;
                showChartContextMenu(event, sample);
                event.consume();
                return;
            }
            dragStartX = event.getX();
        });

        signalChart.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!hasLoadedSignal() || event.getButton() == MouseButton.SECONDARY) {
                return;
            }
            double deltaX = event.getX() - dragStartX;
            if (Math.abs(deltaX) < 30) {
                return;
            }
            saveChartStateForUndo();
            int shift = Math.max(100, currentWindowSize / 5);
            if (deltaX < 0) {
                currentStartIndex = signalWindowService.moveRight(currentRecord.getChannelOneMv(), currentStartIndex, shift);
            } else {
                currentStartIndex = signalWindowService.moveLeft(currentStartIndex, shift);
            }
            dragStartX = event.getX();
            drawCurrentWindow();
            event.consume();
        });

        xAxis.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!hasLoadedSignal()) { return; }
            saveChartStateForUndo();
            xAxisDragStartX = event.getX();
            xAxisDragStartWindowSize = currentWindowSize;
            xAxisDragCenterIndex = currentStartIndex + currentWindowSize / 2;
            event.consume();
        });
        xAxis.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!hasLoadedSignal()) { return; }
            double deltaX = event.getX() - xAxisDragStartX;
            double factor = Math.exp(deltaX / 220.0);
            int newWindowSize = clampWindowSize((int) Math.round(xAxisDragStartWindowSize * factor));
            if (newWindowSize != currentWindowSize) {
                currentWindowSize = newWindowSize;
                currentStartIndex = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), xAxisDragCenterIndex - currentWindowSize / 2, currentWindowSize);
                drawCurrentWindow();
            }
            event.consume();
        });

        yAxis.addEventFilter(MouseEvent.MOUSE_PRESSED, event -> {
            if (!hasLoadedSignal()) { return; }
            saveChartStateForUndo();
            yAxisDragStartY = event.getY();
            yAxisDragStartScaleFactor = yScaleFactor;
            event.consume();
        });
        yAxis.addEventFilter(MouseEvent.MOUSE_DRAGGED, event -> {
            if (!hasLoadedSignal()) { return; }
            double deltaY = event.getY() - yAxisDragStartY;
            double factor = Math.exp(deltaY / 180.0);
            yScaleFactor = clamp(yAxisDragStartScaleFactor * factor, MIN_Y_SCALE_FACTOR, MAX_Y_SCALE_FACTOR);
            drawCurrentWindow();
            event.consume();
        });
    }

    private void showChartContextMenu(MouseEvent event, int sampleIndex) {
        ContextMenu menu = new ContextMenu();

        MenuItem setStart = new MenuItem("Set segment start here");
        setStart.setOnAction(action -> setSegmentStart(sampleIndex));

        MenuItem setEnd = new MenuItem("Set segment end here");
        setEnd.setOnAction(action -> setSegmentEnd(sampleIndex));

        MenuItem analyzeSegment = new MenuItem("Analyze selected segment");
        analyzeSegment.setOnAction(action -> onAnalyzeSelectedSegment());

        MenuItem clearSegment = new MenuItem("Clear segment selection");
        clearSegment.setOnAction(action -> onClearSegmentSelection());

        MenuItem addPeak = new MenuItem("Add R-peak here");
        addPeak.setOnAction(action -> addRPeakAt(sampleIndex));

        MenuItem removePeak = new MenuItem("Remove nearest R-peak");
        removePeak.setOnAction(action -> removeNearestRPeak(sampleIndex));

        MenuItem addAnnotation = new MenuItem("Add annotation here");
        addAnnotation.setOnAction(action -> addUserAnnotationAt(sampleIndex));

        menu.getItems().addAll(setStart, setEnd, analyzeSegment, clearSegment, addPeak, removePeak, addAnnotation);
        menu.show(signalChart, event.getScreenX(), event.getScreenY());
    }

    private void drawCurrentWindow() {
        if (!hasLoadedSignal()) {
            signalChart.getData().clear();
            updateWindowInfo(null);
            return;
        }

        SignalWindow window = signalWindowService.createWindow(currentRecord.getChannelOneMv(), currentStartIndex, currentWindowSize);
        currentStartIndex = window.getStartIndex();

        signalVisualizationService.drawSignal(signalChart, xAxis, yAxis, window, currentRecord.getSamplingFrequency());
        applyYAxisScale();

        drawSegmentMarkers(window);

        if (shouldShowSuspiciousSegments()) {
            drawSuspiciousSegments(window);
        }

        if (shouldShowAnnotationMarkers()) {
            drawAnnotationMarkers(window);
        }
        if (shouldShowRPeakMarkers()) {
            drawRPeakMarkers(window);
        }

        updateWindowInfo(window);
        updateNavigationSlider(window);
    }

    private void drawSuspiciousSegments(SignalWindow window) {
        if (window == null || currentAnalysisResult == null || currentAnalysisResult.getSuspiciousSegments().isEmpty()) {
            return;
        }

        double lower = yAxis.getLowerBound();
        double upper = yAxis.getUpperBound();
        for (SuspiciousSegment segment : currentAnalysisResult.getSuspiciousSegments()) {
            if (!isSuspiciousSegmentInsideAnalysisBounds(segment)) {
                continue;
            }
            int analysisStart = analysisStartIndex >= 0 ? analysisStartIndex : 0;
            int analysisEnd = analysisEndIndex >= 0 ? analysisEndIndex : currentRecord.getChannelOneMv().length - 1;
            int visibleStart = Math.max(Math.max(segment.getStartSampleIndex(), window.getStartIndex()), analysisStart);
            int visibleEnd = Math.min(Math.min(segment.getEndSampleIndex(), window.getEndIndex()), analysisEnd);
            if (visibleEnd <= visibleStart) {
                continue;
            }
            double startTime = (double) visibleStart / currentRecord.getSamplingFrequency();
            double endTime = (double) visibleEnd / currentRecord.getSamplingFrequency();
            XYChart.Series<Number, Number> areaSeries = new XYChart.Series<>();
            areaSeries.getData().add(new XYChart.Data<>(startTime, lower));
            areaSeries.getData().add(new XYChart.Data<>(startTime, upper));
            areaSeries.getData().add(new XYChart.Data<>(endTime, upper));
            areaSeries.getData().add(new XYChart.Data<>(endTime, lower));
            areaSeries.getData().add(new XYChart.Data<>(startTime, lower));
            addLineSeries(areaSeries, "#f57c00", 1.4, "4 4");
        }
    }


    private void drawSegmentMarkers(SignalWindow window) {
        if (window == null || !hasLoadedSignal()) {
            return;
        }

        List<Integer> visibleBorders = new ArrayList<>();
        if (segmentStartIndex >= 0 && segmentStartIndex >= window.getStartIndex() && segmentStartIndex <= window.getEndIndex()) {
            visibleBorders.add(segmentStartIndex);
        }
        if (segmentEndIndex >= 0 && segmentEndIndex != segmentStartIndex && segmentEndIndex >= window.getStartIndex() && segmentEndIndex <= window.getEndIndex()) {
            visibleBorders.add(segmentEndIndex);
        }

        if (visibleBorders.isEmpty()) {
            return;
        }

        double lower = yAxis.getLowerBound();
        double upper = yAxis.getUpperBound();
        for (int border : visibleBorders) {
            XYChart.Series<Number, Number> borderSeries = new XYChart.Series<>();
            double time = (double) border / currentRecord.getSamplingFrequency();
            borderSeries.getData().add(new XYChart.Data<>(time, lower));
            borderSeries.getData().add(new XYChart.Data<>(time, upper));
            addLineSeries(borderSeries, "#7b1fa2", 1.7, "8 5");
        }
    }

    private void addLineSeries(XYChart.Series<Number, Number> series, String color, double width, String dashArray) {
        if (series.getData().isEmpty()) {
            return;
        }
        series.nodeProperty().addListener((observable, oldNode, newNode) -> {
            if (newNode != null) {
                String dash = dashArray == null || dashArray.isBlank() ? "" : " -fx-stroke-dash-array: " + dashArray + ";";
                newNode.setStyle("-fx-stroke: " + color + "; -fx-stroke-width: " + width + ";" + dash);
            }
        });
        signalChart.getData().add(series);
        if (series.getNode() != null) {
            String dash = dashArray == null || dashArray.isBlank() ? "" : " -fx-stroke-dash-array: " + dashArray + ";";
            series.getNode().setStyle("-fx-stroke: " + color + "; -fx-stroke-width: " + width + ";" + dash);
        }
    }

    private void drawRPeakMarkers(SignalWindow window) {
        if (window == null || currentAnalysisResult == null || currentAnalysisResult.getRPeaks().isEmpty()) {
            return;
        }

        XYChart.Series<Number, Number> peakSeries = new XYChart.Series<>();
        for (RPeak peak : currentAnalysisResult.getRPeaks()) {
            if (peak.getSampleIndex() < window.getStartIndex() || peak.getSampleIndex() > window.getEndIndex()) {
                continue;
            }
            double y = peak.getAmplitudeMv();
            if (shouldShowAnnotationMarkers()) {
                y += getMarkerOffset();
            }
            XYChart.Data<Number, Number> point = new XYChart.Data<>(peak.getTimeSeconds(), y);
            point.setNode(createMarkerSymbol(8, "#d32f2f", "white", 1.2));
            peakSeries.getData().add(point);
        }
        addMarkerSeries(peakSeries);
    }

    private void drawAnnotationMarkers(SignalWindow window) {
        if (window == null || currentRecord == null || currentRecord.getAnnotations().isEmpty()) {
            return;
        }

        String selectedType = annotationFilterComboBox.getSelectionModel().getSelectedItem();
        String searchText = annotationSearchField == null || annotationSearchField.getText() == null
                ? ""
                : annotationSearchField.getText().trim().toLowerCase();
        double[] signal = currentRecord.getChannelOneMv();
        XYChart.Series<Number, Number> annotationSeries = new XYChart.Series<>();

        for (ECGAnnotation annotation : currentRecord.getAnnotations()) {
            if (selectedType != null && !selectedType.equals("All") && !selectedType.equals(annotation.getType())) {
                continue;
            }
            if (!matchesAnnotationSearch(annotation, searchText)) {
                continue;
            }
            int markerSampleIndex = findAnnotationDisplaySampleIndex(annotation);
            if (markerSampleIndex < window.getStartIndex() || markerSampleIndex > window.getEndIndex()) {
                continue;
            }
            double timeSeconds = (double) markerSampleIndex / currentRecord.getSamplingFrequency();
            XYChart.Data<Number, Number> point = new XYChart.Data<>(timeSeconds, signal[markerSampleIndex]);
            point.setNode(createAnnotationSymbol(annotation));
            annotationSeries.getData().add(point);
        }
        addMarkerSeries(annotationSeries);
    }

    private void addMarkerSeries(XYChart.Series<Number, Number> series) {
        if (series.getData().isEmpty()) {
            return;
        }
        series.nodeProperty().addListener((observable, oldNode, newNode) -> {
            if (newNode != null) {
                newNode.setStyle("-fx-stroke: transparent; -fx-stroke-width: 0px;");
            }
        });
        signalChart.getData().add(series);
        if (series.getNode() != null) {
            series.getNode().setStyle("-fx-stroke: transparent; -fx-stroke-width: 0px;");
        }
    }

    private void analyzeRange(int from, int to, String label) {
        int start = Math.max(0, Math.min(from, to));
        int end = Math.min(currentRecord.getChannelOneMv().length - 1, Math.max(from, to));
        if (end <= start) {
            setStatus("Selected segment is too short");
            return;
        }

        double[] source = currentRecord.getChannelOneMv();
        double[] segment = new double[end - start + 1];
        System.arraycopy(source, start, segment, 0, segment.length);

        ECGAnalysisResult segmentResult = signalAnalysisService.analyze(segment, currentRecord.getSamplingFrequency());
        currentAnalysisResult = annotateSuspiciousSegments(shiftAnalysisResultToAbsoluteRange(segmentResult, start, source));
        analysisStartIndex = start;
        analysisEndIndex = end;
        suspiciousSegmentCursor = -1;
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.setSelected(true);
        }
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.setSelected(appSettings.isShowRPeaksByDefault() || showRPeaksCheckBox.isSelected());
        }
        if (showSuspiciousSegmentsCheckBox != null) {
            showSuspiciousSegmentsCheckBox.setSelected(!currentAnalysisResult.getSuspiciousSegments().isEmpty());
        }
        showAnalysisResult(segmentResult, label + " " + formatTime(start) + "–" + formatTime(end));
        centerSignalOnSample((start + end) / 2L);
        drawCurrentWindow();
        setStatus(label + " analyzed");
    }


    private ECGAnalysisResult shiftAnalysisResultToAbsoluteRange(ECGAnalysisResult relativeResult, int offset, double[] fullSignal) {
        if (relativeResult == null) {
            return null;
        }
        int samplingFrequency = Math.max(1, currentRecord.getSamplingFrequency());
        List<RPeak> absolutePeaks = relativeResult.getRPeaks().stream()
                .map(peak -> {
                    int sample = Math.max(0, Math.min(fullSignal.length - 1, peak.getSampleIndex() + offset));
                    return new RPeak(sample, (double) sample / samplingFrequency, fullSignal[sample]);
                })
                .toList();
        List<SuspiciousSegment> absoluteSegments = relativeResult.getSuspiciousSegments().stream()
                .map(segment -> {
                    int start = Math.max(0, Math.min(fullSignal.length - 1, segment.getStartSampleIndex() + offset));
                    int end = Math.max(0, Math.min(fullSignal.length - 1, segment.getEndSampleIndex() + offset));
                    return new SuspiciousSegment(
                            start,
                            end,
                            (double) start / samplingFrequency,
                            (double) end / samplingFrequency,
                            segment.getType(),
                            segment.getDescription()
                    );
                })
                .toList();
        return new ECGAnalysisResult(
                relativeResult.getTotalSamples(),
                relativeResult.getDurationSeconds(),
                relativeResult.getMinimumAmplitudeMv(),
                relativeResult.getMaximumAmplitudeMv(),
                relativeResult.getMeanAmplitudeMv(),
                relativeResult.getStandardDeviationMv(),
                absolutePeaks,
                relativeResult.getRrIntervalsSeconds(),
                absoluteSegments,
                relativeResult.getAverageRrIntervalSeconds(),
                relativeResult.getAverageHeartRateBpm(),
                relativeResult.getMinimumHeartRateBpm(),
                relativeResult.getMaximumHeartRateBpm(),
                relativeResult.getSdnnMilliseconds(),
                relativeResult.getRmssdMilliseconds(),
                relativeResult.getPnn50Percent(),
                relativeResult.getRhythmStatus()
        );
    }

    private ECGAnalysisResult annotateSuspiciousSegments(ECGAnalysisResult result) {
        if (result == null || currentRecord == null || currentRecord.getAnnotations() == null || currentRecord.getAnnotations().isEmpty()) {
            return result;
        }
        int samplingFrequency = Math.max(1, currentRecord.getSamplingFrequency());
        List<SuspiciousSegment> enrichedSegments = result.getSuspiciousSegments().stream()
                .map(segment -> {
                    String annotationSummary = findAnnotationSummaryForSegment(segment);
                    if (annotationSummary.isBlank()) {
                        return segment;
                    }
                    return new SuspiciousSegment(
                            segment.getStartSampleIndex(),
                            segment.getEndSampleIndex(),
                            segment.getStartTimeSeconds(),
                            segment.getEndTimeSeconds(),
                            segment.getType(),
                            segment.getDescription() + ". Nearby annotation(s): " + annotationSummary
                    );
                })
                .toList();
        return new ECGAnalysisResult(
                result.getTotalSamples(),
                result.getDurationSeconds(),
                result.getMinimumAmplitudeMv(),
                result.getMaximumAmplitudeMv(),
                result.getMeanAmplitudeMv(),
                result.getStandardDeviationMv(),
                result.getRPeaks(),
                result.getRrIntervalsSeconds(),
                enrichedSegments,
                result.getAverageRrIntervalSeconds(),
                result.getAverageHeartRateBpm(),
                result.getMinimumHeartRateBpm(),
                result.getMaximumHeartRateBpm(),
                result.getSdnnMilliseconds(),
                result.getRmssdMilliseconds(),
                result.getPnn50Percent(),
                result.getRhythmStatus()
        );
    }

    private String findAnnotationSummaryForSegment(SuspiciousSegment segment) {
        if (segment == null || currentRecord == null || currentRecord.getAnnotations() == null) {
            return "";
        }
        int margin = Math.max(1, currentRecord.getSamplingFrequency() / 4);
        return currentRecord.getAnnotations().stream()
                .filter(annotation -> annotation.getSampleIndex() >= segment.getStartSampleIndex() - margin)
                .filter(annotation -> annotation.getSampleIndex() <= segment.getEndSampleIndex() + margin)
                .limit(4)
                .map(annotation -> annotation.getType() + " at " + formatTime((int) annotation.getSampleIndex()))
                .collect(Collectors.joining(", "));
    }

    private void addRPeakAt(int sampleIndex) {
        if (!hasLoadedSignal()) { return; }
        int sample = findLocalMaximumNear(sampleIndex, Math.max(4, currentRecord.getSamplingFrequency() / 20));
        saveUndoSnapshot();
        List<RPeak> peaks = currentAnalysisResult == null ? new ArrayList<>() : new ArrayList<>(currentAnalysisResult.getRPeaks());
        peaks.removeIf(peak -> Math.abs(peak.getSampleIndex() - sample) <= Math.max(3, currentRecord.getSamplingFrequency() / 30));
        peaks.add(new RPeak(sample, (double) sample / currentRecord.getSamplingFrequency(), currentRecord.getChannelOneMv()[sample]));
        updateAnalysisFromManualPeaks(peaks, "R-peak added at " + formatTime(sample));
    }

    private void removeNearestRPeak(int sampleIndex) {
        if (!hasLoadedSignal() || currentAnalysisResult == null || currentAnalysisResult.getRPeaks().isEmpty()) {
            setStatus("Run analysis before removing R-peaks");
            return;
        }
        int maxDistance = Math.max(10, currentRecord.getSamplingFrequency() / 5);
        saveUndoSnapshot();
        List<RPeak> peaks = new ArrayList<>(currentAnalysisResult.getRPeaks());
        Optional<RPeak> nearest = peaks.stream()
                .min(Comparator.comparingInt(peak -> Math.abs(peak.getSampleIndex() - sampleIndex)));
        if (nearest.isEmpty() || Math.abs(nearest.get().getSampleIndex() - sampleIndex) > maxDistance) {
            setStatus("No nearby R-peak found");
            return;
        }
        peaks.remove(nearest.get());
        updateAnalysisFromManualPeaks(peaks, "Nearest R-peak removed");
    }

    private void updateAnalysisFromManualPeaks(List<RPeak> peaks, String status) {
        currentAnalysisResult = signalAnalysisService.analyzeWithRPeaks(currentRecord.getChannelOneMv(), currentRecord.getSamplingFrequency(), peaks);
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.setSelected(true);
        }
        if (showRPeaksCheckBox != null) {
            showRPeaksCheckBox.setSelected(appSettings.isShowRPeaksByDefault() || showRPeaksCheckBox.isSelected());
        }
        if (showSuspiciousSegmentsCheckBox != null) {
            showSuspiciousSegmentsCheckBox.setSelected(!currentAnalysisResult.getSuspiciousSegments().isEmpty());
        }
        showAnalysisResult(currentAnalysisResult, "Manual R-peak set");
        drawCurrentWindow();
        setStatus(status);
    }

    private void addUserAnnotationAt(int sampleIndex) {
        if (!hasLoadedSignal()) { return; }
        TextInputDialog typeDialog = new TextInputDialog("N");
        typeDialog.setTitle("Add annotation");
        typeDialog.setHeaderText("Add user annotation at " + formatTime(sampleIndex));
        typeDialog.setContentText("Annotation type:");
        Optional<String> typeResult = typeDialog.showAndWait();
        if (typeResult.isEmpty() || typeResult.get().isBlank()) {
            setStatus("Annotation creation cancelled");
            return;
        }

        TextInputDialog descriptionDialog = new TextInputDialog("User annotation");
        descriptionDialog.setTitle("Add annotation");
        descriptionDialog.setHeaderText("Description for annotation " + typeResult.get().trim());
        descriptionDialog.setContentText("Description:");
        Optional<String> descriptionResult = descriptionDialog.showAndWait();

        saveUndoSnapshot();
        ECGAnnotation annotation = new ECGAnnotation(
                sampleIndex,
                typeResult.get().trim(),
                descriptionResult.orElse("User annotation").trim().isEmpty() ? "User annotation" : descriptionResult.orElse("User annotation").trim(),
                "User"
        );
        currentRecord.getAnnotations().add(annotation);
        currentRecord.getAnnotations().sort(Comparator.comparingLong(ECGAnnotation::getSampleIndex));
        selectedAnnotationSampleIndex = annotation.getSampleIndex();
        refreshAnnotationFilterOptions(currentRecord.getAnnotations());
        refreshAnnotationTable();
        if (showAnnotationsCheckBox != null) {
            showAnnotationsCheckBox.setSelected(true);
        }
        drawCurrentWindow();
        updateSelectedEvent("User annotation", sampleIndex, annotation.getType(), annotation.getDescription());
        setStatus("Annotation added at sample " + sampleIndex);
    }

    private void loadAnnotations(ECGRecord record) {
        if (record == null || record.getAnnotations() == null || record.getAnnotations().isEmpty()) {
            clearAnnotations();
            return;
        }
        refreshAnnotationFilterOptions(record.getAnnotations());
        refreshAnnotationTable();
        updateAnnotationStats(record.getAnnotations());
    }

    private void refreshAnnotationFilterOptions(List<ECGAnnotation> annotations) {
        String selected = annotationFilterComboBox.getSelectionModel().getSelectedItem();
        Set<String> types = annotations.stream()
                .map(ECGAnnotation::getType)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> options = new ArrayList<>();
        options.add("All");
        options.addAll(types);
        annotationFilterComboBox.setItems(FXCollections.observableArrayList(options));
        if (selected != null && options.contains(selected)) {
            annotationFilterComboBox.getSelectionModel().select(selected);
        } else {
            annotationFilterComboBox.getSelectionModel().select("All");
        }
    }

    private void refreshAnnotationTable() {
        if (currentRecord == null || currentRecord.getAnnotations() == null) {
            annotationsTableView.setItems(FXCollections.observableArrayList());
            return;
        }
        String selectedType = annotationFilterComboBox.getSelectionModel().getSelectedItem();
        String searchText = annotationSearchField == null || annotationSearchField.getText() == null
                ? ""
                : annotationSearchField.getText().trim().toLowerCase();
        List<AnnotationTableRow> rows = currentRecord.getAnnotations().stream()
                .filter(annotation -> selectedType == null || selectedType.equals("All") || selectedType.equals(annotation.getType()))
                .filter(annotation -> matchesAnnotationSearch(annotation, searchText))
                .map(annotation -> new AnnotationTableRow(
                        annotation.getSampleIndex(),
                        (double) annotation.getSampleIndex() / currentRecord.getSamplingFrequency(),
                        annotation.getType(),
                        annotation.getSource(),
                        annotation.getDescription()))
                .toList();
        annotationsTableView.setItems(FXCollections.observableArrayList(rows));
        updateAnnotationStats(currentRecord.getAnnotations());
    }

    private boolean matchesAnnotationSearch(ECGAnnotation annotation, String searchText) {
        if (searchText == null || searchText.isBlank()) {
            return true;
        }
        String sample = Long.toString(annotation.getSampleIndex());
        String time = hasLoadedSignal()
                ? String.format("%.2f", (double) annotation.getSampleIndex() / currentRecord.getSamplingFrequency())
                : "";
        return sample.contains(searchText)
                || time.contains(searchText)
                || containsIgnoreCase(annotation.getType(), searchText)
                || containsIgnoreCase(annotation.getSource(), searchText)
                || containsIgnoreCase(annotation.getDescription(), searchText);
    }

    private boolean containsIgnoreCase(String value, String searchText) {
        return value != null && value.toLowerCase().contains(searchText);
    }

    private void updateAnnotationStats(List<ECGAnnotation> annotations) {
        if (annotations == null || annotations.isEmpty()) {
            annotationStatsLabel.setText("Total: 0 | N: 0 | V: 0 | A: 0 | Other: 0");
            if (annotationsLabel != null) { annotationsLabel.setText("Annotations: 0"); }
            return;
        }
        Map<String, Long> counts = annotations.stream().map(ECGAnnotation::getType).collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        long normal = counts.getOrDefault("N", 0L);
        long pvc = counts.getOrDefault("V", 0L);
        long atrial = counts.getOrDefault("A", 0L) + counts.getOrDefault("a", 0L);
        long other = annotations.size() - normal - pvc - atrial;
        annotationStatsLabel.setText("Total: " + annotations.size() + " | N: " + normal + " | V: " + pvc + " | A: " + atrial + " | Other: " + other);
        if (annotationsLabel != null) { annotationsLabel.setText("Annotations: " + annotations.size()); }
    }

    private void navigateAnnotation(int direction) {
        if (annotationsTableView == null || annotationsTableView.getItems().isEmpty()) {
            setStatus("No annotations available");
            return;
        }
        int index = annotationsTableView.getSelectionModel().getSelectedIndex();
        if (index < 0) {
            index = direction > 0 ? 0 : annotationsTableView.getItems().size() - 1;
        } else {
            index = Math.max(0, Math.min(annotationsTableView.getItems().size() - 1, index + direction));
        }
        annotationsTableView.getSelectionModel().select(index);
        annotationsTableView.scrollTo(index);
    }

    private void showAnalysisResult(ECGAnalysisResult result, String scope) {
        if (result == null || result.getTotalSamples() == 0) {
            clearAnalysis();
            return;
        }
        analysisStatsLabel.setText(scope + ": min " + formatDouble(result.getMinimumAmplitudeMv())
                + " mV | max " + formatDouble(result.getMaximumAmplitudeMv())
                + " mV | mean " + formatDouble(result.getMeanAmplitudeMv()) + " mV");
        heartRateLabel.setText("R-peaks: " + result.getRPeaks().size()
                + " | RR: " + formatDouble(result.getAverageRrIntervalSeconds())
                + " s | HR: " + formatDouble(result.getAverageHeartRateBpm()) + " bpm");
        variabilityLabel.setText("SDNN: " + formatDouble(result.getSdnnMilliseconds())
                + " ms | RMSSD: " + formatDouble(result.getRmssdMilliseconds())
                + " ms | pNN50: " + formatDouble(result.getPnn50Percent()) + "%");
        if (rhythmStatusLabel != null) {
            rhythmStatusLabel.setText("Rhythm: " + result.getRhythmStatus());
        }
        if (suspiciousSegmentsLabel != null) {
            suspiciousSegmentsLabel.setText("Suspicious RR segments: " + result.getSuspiciousSegments().size());
        }
        currentAnalysisScope = scope;
        ECGAnalysisResult metricsSource = currentAnalysisResult != null ? currentAnalysisResult : result;
        currentDetectionMetrics = calculateRPeakDetectionMetrics(metricsSource);
        updateDetectionAccuracyLabel();
        updateContextAnalysis(result);
    }

    private void updateContextAnalysis(ECGAnalysisResult result) {
        if (contextAnalysisLabel == null || result == null || result.getRPeaks().size() < 2) {
            if (contextAnalysisLabel != null) {
                contextAnalysisLabel.setText("Not enough R-peaks for rhythm context.");
            }
            return;
        }

        StringBuilder text = new StringBuilder();
        double hr = result.getAverageHeartRateBpm();
        if (hr < 60.0) {
            text.append("Low average heart rate. ");
        } else if (hr > 100.0) {
            text.append("High average heart rate. ");
        } else {
            text.append("Heart rate is within a typical resting range. ");
        }

        if (result.getRmssdMilliseconds() > 0 && result.getSdnnMilliseconds() > 0) {
            text.append("RR variability can be reviewed through SDNN and RMSSD. ");
        }
        if (!result.getSuspiciousSegments().isEmpty()) {
            SuspiciousSegment first = result.getSuspiciousSegments().get(0);
            text.append("Detected ").append(result.getSuspiciousSegments().size())
                    .append(" suspicious RR segment(s). First: ")
                    .append(first.getType()).append(" at ")
                    .append(String.format("%.2f", first.getStartTimeSeconds()))
                    .append("–")
                    .append(String.format("%.2f", first.getEndTimeSeconds()))
                    .append(" s.");
        } else if (result.getAverageRrIntervalSeconds() > 1.5) {
            text.append("Long RR intervals are present; review the segment visually.");
        } else {
            text.append("Review R-peaks and suspicious RR segments on the graph for confirmation.");
        }
        contextAnalysisLabel.setText(text.toString());
    }

    private void clearAnalysis() {
        currentAnalysisResult = null;
        resetAnalysisNavigation();
        if (analysisStatsLabel != null) { analysisStatsLabel.setText("Amplitude: —"); }
        if (heartRateLabel != null) { heartRateLabel.setText("R-peaks: — | RR: — | HR: —"); }
        if (variabilityLabel != null) { variabilityLabel.setText("SDNN: — | RMSSD: — | pNN50: —"); }
        if (rhythmStatusLabel != null) { rhythmStatusLabel.setText("Rhythm: —"); }
        if (suspiciousSegmentsLabel != null) { suspiciousSegmentsLabel.setText("Suspicious RR segments: —"); }
        if (detectionAccuracyLabel != null) { detectionAccuracyLabel.setText("Detection accuracy: —"); }
        currentDetectionMetrics = RPeakDetectionMetrics.unavailable();
        currentAnalysisScope = "—";
        if (contextAnalysisLabel != null) { contextAnalysisLabel.setText("Load a record and run analysis."); }
    }

    private void clearAnnotations() {
        if (annotationFilterComboBox != null) {
            annotationFilterComboBox.setItems(FXCollections.observableArrayList("All"));
            annotationFilterComboBox.getSelectionModel().select("All");
        }
        if (annotationsTableView != null) {
            annotationsTableView.setItems(FXCollections.observableArrayList());
        }
        selectedAnnotationSampleIndex = -1;
        if (annotationStatsLabel != null) { annotationStatsLabel.setText("Total: 0 | N: 0 | V: 0 | A: 0 | Other: 0"); }
    }

    private void showRecordInfo(ECGRecord record) {
        if (recordNameLabel != null) { recordNameLabel.setText("Record: " + record.getRecordName()); }
        if (channelsLabel != null) { channelsLabel.setText("Lead: " + record.getLeadNames()); }
        if (frequencyLabel != null) { frequencyLabel.setText("Frequency: " + record.getSamplingFrequency() + " Hz"); }
        if (durationLabel != null) { durationLabel.setText("Duration: " + formatDuration(record)); }
        if (annotationsLabel != null) { annotationsLabel.setText("Annotations: " + record.getAnnotations().size()); }
        if (samplesLabel != null) { samplesLabel.setText("Samples: " + record.getSampleCount()); }
        if (signalFormatLabel != null) { signalFormatLabel.setText("Signal format: " + record.getSignalFormats()); }
        if (gainLabel != null) { gainLabel.setText("Gain: " + record.getGains()); }
        if (baselineLabel != null) { baselineLabel.setText("Baseline: " + record.getBaselines()); }
        if (signalFileLabel != null) { signalFileLabel.setText("Signal file: " + record.getSignalFile().getName()); }
        if (annotationFileLabel != null) { annotationFileLabel.setText(record.getAnnotationFile() == null ? "Annotation file: not found" : "Annotation file: " + record.getAnnotationFile().getName()); }
    }

    private void clearRecordInfo() {
        if (recordNameLabel != null) { recordNameLabel.setText("Record: —"); }
        if (channelsLabel != null) { channelsLabel.setText("Lead: —"); }
        if (frequencyLabel != null) { frequencyLabel.setText("Frequency: —"); }
        if (durationLabel != null) { durationLabel.setText("Duration: —"); }
        if (annotationsLabel != null) { annotationsLabel.setText("Annotations: —"); }
        if (samplesLabel != null) { samplesLabel.setText("Samples: —"); }
        if (signalFormatLabel != null) { signalFormatLabel.setText("Signal format: —"); }
        if (gainLabel != null) { gainLabel.setText("Gain: —"); }
        if (baselineLabel != null) { baselineLabel.setText("Baseline: —"); }
        if (signalFileLabel != null) { signalFileLabel.setText("Signal file: —"); }
        if (annotationFileLabel != null) { annotationFileLabel.setText("Annotation file: —"); }
        if (scaleInfoLabel != null) { scaleInfoLabel.setText("Scale: —"); }
        if (selectedEventLabel != null) { selectedEventLabel.setText("Selected: —"); }
    }

    private void updateNavigationSlider(SignalWindow window) {
        if (navigationSlider == null) { return; }
        updatingNavigationSlider = true;
        try {
            if (window == null || !hasLoadedSignal()) {
                navigationSlider.setDisable(true);
                navigationSlider.setMin(0);
                navigationSlider.setMax(0);
                navigationSlider.setValue(0);
                return;
            }
            int maxStartIndex = Math.max(0, currentRecord.getChannelOneMv().length - currentWindowSize);
            navigationSlider.setDisable(maxStartIndex == 0);
            navigationSlider.setMin(0);
            navigationSlider.setMax(maxStartIndex);
            navigationSlider.setValue(currentStartIndex);
            navigationSlider.setBlockIncrement(Math.max(1, currentWindowSize));
        } finally {
            updatingNavigationSlider = false;
        }
    }

    private void updateWindowInfo(SignalWindow window) {
        if (window == null || !hasLoadedSignal()) {
            if (windowInfoLabel != null) { windowInfoLabel.setText("Window: —"); }
            if (scaleInfoLabel != null) { scaleInfoLabel.setText("Scale: —"); }
            return;
        }
        double startTime = (double) window.getStartIndex() / currentRecord.getSamplingFrequency();
        double endTime = (double) window.getEndIndex() / currentRecord.getSamplingFrequency();
        windowInfoLabel.setText("Window: " + String.format("%.2f", startTime) + "–" + String.format("%.2f", endTime) + " s");
        scaleInfoLabel.setText("Samples: " + window.size() + " | Y scale " + String.format("%.2fx", yScaleFactor));
    }

    private void saveChartStateForUndo() {
        if (!hasLoadedSignal() || restoringChartState) {
            return;
        }
        ChartState currentState = captureChartState();
        if (!chartUndoStack.isEmpty() && chartUndoStack.peek().equals(currentState)) {
            return;
        }
        chartUndoStack.push(currentState);
        chartRedoStack.clear();
        while (chartUndoStack.size() > MAX_NAVIGATION_HISTORY) {
            chartUndoStack.removeLast();
        }
    }

    private void undoChartAction() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before undo");
            return;
        }
        if (chartUndoStack.isEmpty()) {
            setStatus("No navigation actions to undo");
            return;
        }
        chartRedoStack.push(captureChartState());
        restoreChartState(chartUndoStack.pop());
        setStatus("Navigation action undone");
    }

    private void redoChartAction() {
        if (!hasLoadedSignal()) {
            setStatus("Load ECG record before redo");
            return;
        }
        if (chartRedoStack.isEmpty()) {
            setStatus("No navigation actions to redo");
            return;
        }
        chartUndoStack.push(captureChartState());
        restoreChartState(chartRedoStack.pop());
        setStatus("Navigation action redone");
    }

    private ChartState captureChartState() {
        return new ChartState(
                currentStartIndex,
                currentWindowSize,
                yScaleFactor,
                segmentStartIndex,
                segmentEndIndex,
                selectedAnnotationSampleIndex
        );
    }

    private void restoreChartState(ChartState state) {
        if (!hasLoadedSignal() || state == null) {
            return;
        }
        restoringChartState = true;
        try {
            currentWindowSize = clampWindowSize(state.windowSize());
            currentStartIndex = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), state.startIndex(), currentWindowSize);
            yScaleFactor = clamp(state.yScaleFactor(), MIN_Y_SCALE_FACTOR, MAX_Y_SCALE_FACTOR);
            segmentStartIndex = state.segmentStartIndex();
            segmentEndIndex = state.segmentEndIndex();
            selectedAnnotationSampleIndex = state.selectedAnnotationSampleIndex();
            updateSegmentInfo();
            drawCurrentWindow();
            if (annotationsTableView != null) {
                annotationsTableView.refresh();
            }
        } finally {
            restoringChartState = false;
        }
    }

    private void updateSegmentInfo() {
        if (segmentInfoLabel == null) { return; }
        if (!hasLoadedSignal()) {
            segmentInfoLabel.setText("Segment: —");
            return;
        }
        if (hasValidSegment()) {
            segmentInfoLabel.setText("Segment: " + formatTime(getSegmentFrom()) + "–" + formatTime(getSegmentTo()));
            return;
        }
        if (segmentStartIndex >= 0) {
            segmentInfoLabel.setText("Segment start: " + formatTime(segmentStartIndex));
            return;
        }
        if (segmentEndIndex >= 0) {
            segmentInfoLabel.setText("Segment end: " + formatTime(segmentEndIndex));
            return;
        }
        segmentInfoLabel.setText("Segment: —");
    }

    private void updateSelectedEvent(String source, int sample, String type, String description) {
        if (selectedEventLabel == null || !hasLoadedSignal()) { return; }
        selectedEventLabel.setText("Selected: " + source + " | " + formatTime(sample) + " | " + type + " | " + description);
    }

    private void moveToSliderPosition() {
        if (updatingNavigationSlider || !hasLoadedSignal()) { return; }
        int newStartIndex = (int) Math.round(navigationSlider.getValue());
        newStartIndex = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), newStartIndex, currentWindowSize);
        if (newStartIndex != currentStartIndex) {
            saveChartStateForUndo();
            currentStartIndex = newStartIndex;
            drawCurrentWindow();
            setStatus("Signal window moved");
        }
    }

    private void moveToStart(int start, String status) {
        if (!hasLoadedSignal()) { setStatus("Load ECG record before navigation"); return; }
        int targetStart = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), start, currentWindowSize);
        if (targetStart != currentStartIndex) {
            saveChartStateForUndo();
        }
        currentStartIndex = targetStart;
        drawCurrentWindow();
        setStatus(status);
    }

    private void centerSignalOnSample(long sampleIndex) {
        if (!hasLoadedSignal()) { return; }
        int sample = (int) Math.max(0, Math.min(sampleIndex, currentRecord.getChannelOneMv().length - 1));
        int targetStart = signalWindowService.clampStartIndex(currentRecord.getChannelOneMv(), sample - currentWindowSize / 2, currentWindowSize);
        if (targetStart != currentStartIndex) {
            saveChartStateForUndo();
        }
        currentStartIndex = targetStart;
        drawCurrentWindow();
    }

    private void focusSuspiciousSegment(SuspiciousSegment segment, int centerSample) {
        if (!hasLoadedSignal() || segment == null) {
            return;
        }
        int samplingFrequency = Math.max(1, currentRecord.getSamplingFrequency());
        int segmentWidth = Math.max(1, segment.getEndSampleIndex() - segment.getStartSampleIndex() + 1);
        int preferredWindow = Math.max(MIN_WINDOW_SIZE, Math.max(segmentWidth * 6, samplingFrequency * 8));
        currentWindowSize = clampWindowSize(Math.min(currentWindowSize, preferredWindow));

        int margin = Math.max(samplingFrequency, (currentWindowSize - segmentWidth) / 2);
        int start = Math.max(0, segment.getStartSampleIndex() - margin);
        int centeredStart = centerSample - currentWindowSize / 2;
        currentStartIndex = signalWindowService.clampStartIndex(
                currentRecord.getChannelOneMv(),
                Math.min(start, centeredStart),
                currentWindowSize
        );
    }

    private int sampleFromChartMouseX(double mouseX) {
        Point2D axisPoint = xAxis.sceneToLocal(signalChart.localToScene(mouseX, 0));
        Number axisValue = xAxis.getValueForDisplay(axisPoint.getX());
        double timeSeconds;
        if (axisValue == null || Double.isNaN(axisValue.doubleValue()) || Double.isInfinite(axisValue.doubleValue())) {
            double width = Math.max(1.0, signalChart.getWidth());
            double relativeX = clamp(mouseX / width, 0.0, 1.0);
            timeSeconds = xAxis.getLowerBound() + (xAxis.getUpperBound() - xAxis.getLowerBound()) * relativeX;
        } else {
            timeSeconds = axisValue.doubleValue();
        }
        return (int) Math.max(0, Math.min(currentRecord.getChannelOneMv().length - 1, Math.round(timeSeconds * currentRecord.getSamplingFrequency())));
    }

    private void setSegmentStart(int sample) {
        saveChartStateForUndo();
        segmentStartIndex = clampSample(sample);
        if (segmentEndIndex >= 0 && segmentEndIndex < segmentStartIndex) {
            int temp = segmentEndIndex;
            segmentEndIndex = segmentStartIndex;
            segmentStartIndex = temp;
        }
        updateSegmentInfo();
        drawCurrentWindow();
        setStatus("Segment start set: " + formatTime(segmentStartIndex));
    }

    private void setSegmentEnd(int sample) {
        saveChartStateForUndo();
        segmentEndIndex = clampSample(sample);
        if (segmentStartIndex >= 0 && segmentEndIndex < segmentStartIndex) {
            int temp = segmentStartIndex;
            segmentStartIndex = segmentEndIndex;
            segmentEndIndex = temp;
        }
        updateSegmentInfo();
        drawCurrentWindow();
        setStatus("Segment end set: " + formatTime(segmentEndIndex));
    }

    private boolean hasValidSegment() {
        return segmentStartIndex >= 0 && segmentEndIndex >= 0 && segmentStartIndex != segmentEndIndex;
    }

    private int getSegmentFrom() { return Math.min(segmentStartIndex, segmentEndIndex); }
    private int getSegmentTo() { return Math.max(segmentStartIndex, segmentEndIndex); }


    private List<RPeak> buildAnnotationBasedRPeaks(int from, int to) {
        if (!hasLoadedSignal() || currentRecord.getAnnotations() == null || currentRecord.getAnnotations().isEmpty()) {
            return List.of();
        }

        int start = Math.max(0, Math.min(from, to));
        int end = Math.min(currentRecord.getChannelOneMv().length - 1, Math.max(from, to));

        return currentRecord.getAnnotations().stream()
                .filter(annotation -> isBeatAnnotation(annotation.getType()))
                .map(annotation -> (int) annotation.getSampleIndex())
                .filter(sample -> sample >= start && sample <= end)
                .map(this::clampSample)
                .distinct()
                .sorted()
                .map(sample -> new RPeak(
                        sample,
                        (double) sample / currentRecord.getSamplingFrequency(),
                        currentRecord.getChannelOneMv()[sample]
                ))
                .toList();
    }

    private boolean isBeatAnnotation(String type) {
        if (type == null || type.isBlank()) {
            return false;
        }
        return switch (type.trim()) {
            case "N", "L", "R", "B", "A", "a", "J", "S", "V", "r",
                 "F", "e", "j", "E", "/", "f", "Q" -> true;
            default -> false;
        };
    }

    private int findLocalMaximumNear(int sampleIndex, int radius) {
        int original = clampSample(sampleIndex);
        int from = Math.max(0, original - radius);
        int to = Math.min(currentRecord.getChannelOneMv().length - 1, original + radius);
        int best = original;
        for (int i = from; i <= to; i++) {
            if (currentRecord.getChannelOneMv()[i] > currentRecord.getChannelOneMv()[best]) {
                best = i;
            }
        }
        return best;
    }

    private int findAnnotationDisplaySampleIndex(ECGAnnotation annotation) {
        if (!hasLoadedSignal() || annotation == null) { return -1; }
        return clampSample((int) annotation.getSampleIndex());
    }

    private StackPane createAnnotationSymbol(ECGAnnotation annotation) {
        boolean selected = annotation.getSampleIndex() == selectedAnnotationSampleIndex;
        double size = selected ? 11 : 7;
        String color = annotation.isUserDefined() ? "#ff8f00" : switch (annotation.getType()) {
            case "N" -> "#2e7d32";
            case "V" -> "#c62828";
            case "A", "a" -> "#ef6c00";
            case "L", "R" -> "#1565c0";
            default -> "#6a1b9a";
        };
        return annotation.isUserDefined()
                ? createSquareMarkerSymbol(size + 1, color, selected ? "#212121" : "white", selected ? 1.8 : 1.0)
                : createMarkerSymbol(size, color, selected ? "#212121" : "white", selected ? 1.8 : 1.0);
    }

    private StackPane createMarkerSymbol(double size, String fillColor, String borderColor, double borderWidth) {
        StackPane symbol = new StackPane();
        symbol.setMinSize(size, size);
        symbol.setPrefSize(size, size);
        symbol.setMaxSize(size, size);
        symbol.setStyle("-fx-background-color: " + fillColor + ";"
                + " -fx-background-radius: " + size + ";"
                + " -fx-border-color: " + borderColor + ";"
                + " -fx-border-width: " + borderWidth + ";"
                + " -fx-border-radius: " + size + ";");
        return symbol;
    }


    private StackPane createSquareMarkerSymbol(double size, String fillColor, String borderColor, double borderWidth) {
        StackPane symbol = new StackPane();
        symbol.setMinSize(size, size);
        symbol.setPrefSize(size, size);
        symbol.setMaxSize(size, size);
        symbol.setStyle("-fx-background-color: " + fillColor + ";"
                + " -fx-background-radius: 1;"
                + " -fx-border-color: " + borderColor + ";"
                + " -fx-border-width: " + borderWidth + ";"
                + " -fx-border-radius: 1;");
        return symbol;
    }

    private double getMarkerOffset() {
        double range = yAxis.getUpperBound() - yAxis.getLowerBound();
        if (range <= 0 || Double.isNaN(range) || Double.isInfinite(range)) {
            return 0.03;
        }
        return range * 0.025;
    }

    private void saveUndoSnapshot() {
        if (!hasLoadedSignal()) {
            return;
        }
        List<RPeak> peaks = currentAnalysisResult == null
                ? new ArrayList<>()
                : new ArrayList<>(currentAnalysisResult.getRPeaks());
        List<ECGAnnotation> annotations = currentRecord.getAnnotations() == null
                ? new ArrayList<>()
                : new ArrayList<>(currentRecord.getAnnotations());
        undoStack.push(new UndoSnapshot(peaks, annotations));
        while (undoStack.size() > 20) {
            undoStack.removeLast();
        }
    }

    private record UndoSnapshot(List<RPeak> peaks, List<ECGAnnotation> annotations) {
    }

    private record ChartState(
            int startIndex,
            int windowSize,
            double yScaleFactor,
            int segmentStartIndex,
            int segmentEndIndex,
            long selectedAnnotationSampleIndex
    ) {
    }

    private void applyYAxisScale() {
        if (yScaleFactor == 1.0) { return; }
        double lower = yAxis.getLowerBound();
        double upper = yAxis.getUpperBound();
        double center = (lower + upper) / 2.0;
        double halfRange = (upper - lower) * yScaleFactor / 2.0;
        if (halfRange <= 0) { return; }
        yAxis.setAutoRanging(false);
        yAxis.setLowerBound(center - halfRange);
        yAxis.setUpperBound(center + halfRange);
        yAxis.setTickUnit(Math.max(0.05, (halfRange * 2.0) / 5.0));
    }

    private int clampWindowSize(int windowSize) {
        if (!hasLoadedSignal()) { return windowSize; }
        int maxSize = Math.min(MAX_WINDOW_SIZE, currentRecord.getChannelOneMv().length);
        return Math.max(MIN_WINDOW_SIZE, Math.min(maxSize, windowSize));
    }

    private int clampSample(int sample) {
        return (int) Math.max(0, Math.min(sample, currentRecord.getChannelOneMv().length - 1));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private boolean isSuspiciousSegmentInsideAnalysisBounds(SuspiciousSegment segment) {
        if (segment == null) {
            return false;
        }
        if (analysisStartIndex < 0 || analysisEndIndex < 0) {
            return true;
        }
        return segment.getEndSampleIndex() >= analysisStartIndex
                && segment.getStartSampleIndex() <= analysisEndIndex;
    }

    private void resetAnalysisNavigation() {
        analysisStartIndex = -1;
        analysisEndIndex = -1;
        suspiciousSegmentCursor = -1;
    }

    private boolean shouldShowAnnotationMarkers() {
        return showAnnotationsCheckBox != null && showAnnotationsCheckBox.isSelected();
    }

    private boolean shouldShowSuspiciousSegments() {
        return showSuspiciousSegmentsCheckBox != null && showSuspiciousSegmentsCheckBox.isSelected();
    }

    private boolean shouldShowRPeakMarkers() {
        return showRPeaksCheckBox != null && showRPeaksCheckBox.isSelected();
    }

    private boolean hasLoadedSignal() {
        return currentRecord != null && currentRecord.getChannelOneMv() != null && currentRecord.getChannelOneMv().length > 0;
    }

    private String formatDouble(double value) {
        if (Double.isNaN(value) || Double.isInfinite(value)) { return "0.00"; }
        return String.format("%.2f", value);
    }

    private String formatTime(int sample) {
        if (!hasLoadedSignal()) { return "—"; }
        return String.format("%.2f s", (double) sample / currentRecord.getSamplingFrequency());
    }

    private String formatDuration(ECGRecord record) {
        if (record == null || record.getSamplingFrequency() <= 0) { return "—"; }
        long totalSeconds = Math.round((double) record.getSampleCount() / record.getSamplingFrequency());
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;
        return minutes + " min " + seconds + " s";
    }

    private RPeakDetectionMetrics calculateRPeakDetectionMetrics(ECGAnalysisResult result) {
        if (result == null || currentRecord == null || currentRecord.getAnnotations() == null || currentRecord.getAnnotations().isEmpty()) {
            return RPeakDetectionMetrics.unavailable();
        }

        int start = analysisStartIndex >= 0 ? analysisStartIndex : 0;
        int end = analysisEndIndex >= 0 ? analysisEndIndex : currentRecord.getChannelOneMv().length - 1;
        List<Integer> referencePeaks = currentRecord.getAnnotations().stream()
                .filter(annotation -> isBeatAnnotation(annotation.getType()))
                .map(annotation -> (int) annotation.getSampleIndex())
                .filter(sample -> sample >= start && sample <= end)
                .sorted()
                .toList();

        if (referencePeaks.isEmpty()) {
            return RPeakDetectionMetrics.unavailable();
        }

        List<Integer> detectedPeaks = result.getRPeaks().stream()
                .map(RPeak::getSampleIndex)
                .filter(sample -> sample >= start && sample <= end)
                .sorted()
                .toList();

        int tolerance = Math.max(1, (int) Math.round(0.15 * currentRecord.getSamplingFrequency()));
        boolean[] matchedDetected = new boolean[detectedPeaks.size()];
        int truePositive = 0;

        for (int reference : referencePeaks) {
            int bestIndex = -1;
            int bestDistance = tolerance + 1;
            for (int i = 0; i < detectedPeaks.size(); i++) {
                if (matchedDetected[i]) {
                    continue;
                }
                int distance = Math.abs(detectedPeaks.get(i) - reference);
                if (distance <= tolerance && distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }
            if (bestIndex >= 0) {
                matchedDetected[bestIndex] = true;
                truePositive++;
            }
        }

        int falsePositive = detectedPeaks.size() - truePositive;
        int falseNegative = referencePeaks.size() - truePositive;
        double sensitivity = referencePeaks.isEmpty() ? 0.0 : truePositive * 100.0 / referencePeaks.size();
        double positivePredictiveValue = detectedPeaks.isEmpty() ? 0.0 : truePositive * 100.0 / detectedPeaks.size();

        return new RPeakDetectionMetrics(
                true,
                referencePeaks.size(),
                detectedPeaks.size(),
                truePositive,
                falsePositive,
                falseNegative,
                sensitivity,
                positivePredictiveValue,
                tolerance
        );
    }

    private void updateDetectionAccuracyLabel() {
        if (detectionAccuracyLabel == null) {
            return;
        }
        if (currentDetectionMetrics == null || !currentDetectionMetrics.available()) {
            detectionAccuracyLabel.setText("Detection accuracy: no reference annotations");
            return;
        }
        detectionAccuracyLabel.setText("Detection accuracy: TP " + currentDetectionMetrics.truePositive()
                + " | FP " + currentDetectionMetrics.falsePositive()
                + " | FN " + currentDetectionMetrics.falseNegative()
                + " | Se " + formatDouble(currentDetectionMetrics.sensitivityPercent())
                + "% | PPV " + formatDouble(currentDetectionMetrics.positivePredictiveValuePercent()) + "%");
    }

    private String buildAnnotationsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("sample,time_seconds,type,source,description\n");
        if (currentRecord == null || currentRecord.getAnnotations() == null) {
            return csv.toString();
        }
        for (ECGAnnotation annotation : currentRecord.getAnnotations()) {
            double timeSeconds = currentRecord.getSamplingFrequency() > 0
                    ? (double) annotation.getSampleIndex() / currentRecord.getSamplingFrequency()
                    : 0.0;
            csv.append(annotation.getSampleIndex()).append(',')
                    .append(String.format("%.6f", timeSeconds)).append(',')
                    .append(escapeCsv(annotation.getType())).append(',')
                    .append(escapeCsv(annotation.getSource())).append(',')
                    .append(escapeCsv(annotation.getDescription())).append('\n');
        }
        return csv.toString();
    }

    private String buildRrIntervalsCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("index,left_r_peak_sample,right_r_peak_sample,left_time_seconds,right_time_seconds,rr_seconds,heart_rate_bpm\n");
        if (currentAnalysisResult == null) {
            return csv.toString();
        }
        List<RPeak> peaks = currentAnalysisResult.getRPeaks();
        List<Double> intervals = currentAnalysisResult.getRrIntervalsSeconds();
        int rows = Math.min(intervals.size(), Math.max(0, peaks.size() - 1));
        for (int i = 0; i < rows; i++) {
            RPeak left = peaks.get(i);
            RPeak right = peaks.get(i + 1);
            double rr = intervals.get(i);
            double hr = rr > 0.0 ? 60.0 / rr : 0.0;
            csv.append(i + 1).append(',')
                    .append(left.getSampleIndex()).append(',')
                    .append(right.getSampleIndex()).append(',')
                    .append(String.format("%.6f", left.getTimeSeconds())).append(',')
                    .append(String.format("%.6f", right.getTimeSeconds())).append(',')
                    .append(String.format("%.6f", rr)).append(',')
                    .append(String.format("%.2f", hr)).append('\n');
        }
        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }


    private String buildAnalysisSummaryCsv() {
        StringBuilder csv = new StringBuilder();
        csv.append("metric,value,unit\n");
        csv.append("record,").append(escapeCsv(currentRecord == null ? "" : currentRecord.getRecordName())).append(",\n");
        csv.append("scope,").append(escapeCsv(currentAnalysisScope)).append(",\n");
        csv.append("sampling_frequency,").append(currentRecord == null ? "" : currentRecord.getSamplingFrequency()).append(",Hz\n");
        csv.append("total_samples,").append(currentAnalysisResult.getTotalSamples()).append(",samples\n");
        csv.append("duration,").append(String.format("%.6f", currentAnalysisResult.getDurationSeconds())).append(",s\n");
        csv.append("minimum_amplitude,").append(String.format("%.6f", currentAnalysisResult.getMinimumAmplitudeMv())).append(",mV\n");
        csv.append("maximum_amplitude,").append(String.format("%.6f", currentAnalysisResult.getMaximumAmplitudeMv())).append(",mV\n");
        csv.append("mean_amplitude,").append(String.format("%.6f", currentAnalysisResult.getMeanAmplitudeMv())).append(",mV\n");
        csv.append("standard_deviation_amplitude,").append(String.format("%.6f", currentAnalysisResult.getStandardDeviationMv())).append(",mV\n");
        csv.append("r_peaks,").append(currentAnalysisResult.getRPeaks().size()).append(",count\n");
        csv.append("rr_intervals,").append(currentAnalysisResult.getRrIntervalsSeconds().size()).append(",count\n");
        csv.append("average_rr,").append(String.format("%.6f", currentAnalysisResult.getAverageRrIntervalSeconds())).append(",s\n");
        csv.append("average_hr,").append(String.format("%.6f", currentAnalysisResult.getAverageHeartRateBpm())).append(",bpm\n");
        csv.append("minimum_hr,").append(String.format("%.6f", currentAnalysisResult.getMinimumHeartRateBpm())).append(",bpm\n");
        csv.append("maximum_hr,").append(String.format("%.6f", currentAnalysisResult.getMaximumHeartRateBpm())).append(",bpm\n");
        csv.append("sdnn,").append(String.format("%.6f", currentAnalysisResult.getSdnnMilliseconds())).append(",ms\n");
        csv.append("rmssd,").append(String.format("%.6f", currentAnalysisResult.getRmssdMilliseconds())).append(",ms\n");
        csv.append("pnn50,").append(String.format("%.6f", currentAnalysisResult.getPnn50Percent())).append(",percent\n");
        csv.append("suspicious_segments,").append(currentAnalysisResult.getSuspiciousSegments().size()).append(",count\n");
        csv.append("rhythm_status,").append(escapeCsv(currentAnalysisResult.getRhythmStatus())).append(",\n");
        if (currentDetectionMetrics != null && currentDetectionMetrics.available()) {
            csv.append("reference_annotations,").append(currentDetectionMetrics.referenceCount()).append(",count\n");
            csv.append("true_positive,").append(currentDetectionMetrics.truePositive()).append(",count\n");
            csv.append("false_positive,").append(currentDetectionMetrics.falsePositive()).append(",count\n");
            csv.append("false_negative,").append(currentDetectionMetrics.falseNegative()).append(",count\n");
            csv.append("sensitivity,").append(String.format("%.6f", currentDetectionMetrics.sensitivityPercent())).append(",percent\n");
            csv.append("positive_predictive_value,").append(String.format("%.6f", currentDetectionMetrics.positivePredictiveValuePercent())).append(",percent\n");
        }
        return csv.toString();
    }

    private String buildAnalysisReport() {
        if (currentAnalysisResult == null) {
            return "No analysis result.";
        }

        StringBuilder report = new StringBuilder();
        report.append("ECG ANALYSIS REPORT\n");
        report.append("===================\n\n");
        report.append("Record: ").append(currentRecord == null ? "—" : currentRecord.getRecordName()).append("\n");
        report.append("Scope: ").append(currentAnalysisScope).append("\n");
        if (analysisStartIndex >= 0 && analysisEndIndex >= 0) {
            report.append("Analyzed samples: ").append(analysisStartIndex).append("–").append(analysisEndIndex)
                    .append(" (").append(formatTime(analysisStartIndex)).append("–").append(formatTime(analysisEndIndex)).append(")\n");
        }
        report.append("Sampling frequency: ").append(currentRecord == null ? "—" : currentRecord.getSamplingFrequency() + " Hz").append("\n\n");

        report.append("Signal statistics\n");
        report.append("- Total analyzed samples: ").append(currentAnalysisResult.getTotalSamples()).append("\n");
        report.append("- Duration: ").append(formatDouble(currentAnalysisResult.getDurationSeconds())).append(" s\n");
        report.append("- Min amplitude: ").append(formatDouble(currentAnalysisResult.getMinimumAmplitudeMv())).append(" mV\n");
        report.append("- Max amplitude: ").append(formatDouble(currentAnalysisResult.getMaximumAmplitudeMv())).append(" mV\n");
        report.append("- Mean amplitude: ").append(formatDouble(currentAnalysisResult.getMeanAmplitudeMv())).append(" mV\n\n");

        report.append("Rhythm analysis\n");
        report.append("- Detected R-peaks: ").append(currentAnalysisResult.getRPeaks().size()).append("\n");
        report.append("- Average RR: ").append(formatDouble(currentAnalysisResult.getAverageRrIntervalSeconds())).append(" s\n");
        report.append("- Average HR: ").append(formatDouble(currentAnalysisResult.getAverageHeartRateBpm())).append(" bpm\n");
        report.append("- Min HR: ").append(formatDouble(currentAnalysisResult.getMinimumHeartRateBpm())).append(" bpm\n");
        report.append("- Max HR: ").append(formatDouble(currentAnalysisResult.getMaximumHeartRateBpm())).append(" bpm\n");
        report.append("- SDNN: ").append(formatDouble(currentAnalysisResult.getSdnnMilliseconds())).append(" ms\n");
        report.append("- RMSSD: ").append(formatDouble(currentAnalysisResult.getRmssdMilliseconds())).append(" ms\n");
        report.append("- pNN50: ").append(formatDouble(currentAnalysisResult.getPnn50Percent())).append("%\n");
        report.append("- Rhythm status: ").append(currentAnalysisResult.getRhythmStatus()).append("\n\n");

        report.append("Suspicious segments\n");
        if (currentAnalysisResult.getSuspiciousSegments().isEmpty()) {
            report.append("- No suspicious segments detected.\n\n");
        } else {
            for (int i = 0; i < currentAnalysisResult.getSuspiciousSegments().size(); i++) {
                SuspiciousSegment segment = currentAnalysisResult.getSuspiciousSegments().get(i);
                report.append("- ").append(i + 1).append(") ")
                        .append(segment.getType()).append(" | ")
                        .append(segment.getStartSampleIndex()).append("–").append(segment.getEndSampleIndex())
                        .append(" | ").append(formatDouble(segment.getStartTimeSeconds())).append("–")
                        .append(formatDouble(segment.getEndTimeSeconds())).append(" s | ")
                        .append(segment.getDescription()).append("\n");
            }
            report.append("\n");
        }

        report.append("R-peak detection validation\n");
        if (currentDetectionMetrics == null || !currentDetectionMetrics.available()) {
            report.append("- Reference annotations are unavailable for this analyzed range.\n");
            report.append("- The algorithm still works because R-peaks and suspicious segments are detected from the ECG signal itself.\n");
        } else {
            report.append("- Reference beat annotations: ").append(currentDetectionMetrics.referenceCount()).append("\n");
            report.append("- Detected R-peaks in range: ").append(currentDetectionMetrics.detectedCount()).append("\n");
            report.append("- True positives: ").append(currentDetectionMetrics.truePositive()).append("\n");
            report.append("- False positives: ").append(currentDetectionMetrics.falsePositive()).append("\n");
            report.append("- False negatives: ").append(currentDetectionMetrics.falseNegative()).append("\n");
            report.append("- Sensitivity: ").append(formatDouble(currentDetectionMetrics.sensitivityPercent())).append("%\n");
            report.append("- Positive predictive value: ").append(formatDouble(currentDetectionMetrics.positivePredictiveValuePercent())).append("%\n");
            report.append("- Matching tolerance: ±").append(currentDetectionMetrics.toleranceSamples()).append(" samples\n");
            report.append("- Note: annotations are used only for validation, not for R-peak or suspicious-zone detection.\n");
        }

        report.append("\nAnalysis limitations\n");
        report.append("- The presented analysis is intended for research and educational purposes.\n");
        report.append("- The software does not provide a medical diagnosis and should not replace professional clinical assessment.\n");

        return report.toString();
    }

    private void loadSavedRecords() {
        if (recordsListView == null) { return; }
        List<String> items = recordRepository.findAllSummaries().stream()
                .map(summary -> summary.getRecordName() + " | " + summary.getSamplingFrequency() + " Hz | annotations: " + summary.getAnnotationsCount())
                .toList();
        recordsListView.setItems(FXCollections.observableArrayList(items));
    }

    private void setStatus(String message) {
        if (statusLabel != null) {
            statusLabel.setText("Status: " + message);
        }
    }

    private record RPeakDetectionMetrics(
            boolean available,
            int referenceCount,
            int detectedCount,
            int truePositive,
            int falsePositive,
            int falseNegative,
            double sensitivityPercent,
            double positivePredictiveValuePercent,
            int toleranceSamples
    ) {
        private static RPeakDetectionMetrics unavailable() {
            return new RPeakDetectionMetrics(false, 0, 0, 0, 0, 0, 0.0, 0.0, 0);
        }
    }
}
