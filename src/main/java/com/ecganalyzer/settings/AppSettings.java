package com.ecganalyzer.settings;

public class AppSettings {

    private int defaultWindowSize;
    private boolean showGridByDefault;
    private boolean showAnnotationsByDefault;
    private boolean showRPeaksByDefault;
    private boolean showSuspiciousSegmentsByDefault;
    private String defaultAnalysisMode;

    public AppSettings() {
        this.defaultWindowSize = 5000;
        this.showGridByDefault = true;
        this.showAnnotationsByDefault = false;
        this.showRPeaksByDefault = false;
        this.showSuspiciousSegmentsByDefault = false;
        this.defaultAnalysisMode = "Full signal";
    }

    public int getDefaultWindowSize() {
        return defaultWindowSize;
    }

    public void setDefaultWindowSize(int defaultWindowSize) {
        this.defaultWindowSize = defaultWindowSize;
    }

    public boolean isShowGridByDefault() {
        return showGridByDefault;
    }

    public void setShowGridByDefault(boolean showGridByDefault) {
        this.showGridByDefault = showGridByDefault;
    }

    public boolean isShowAnnotationsByDefault() {
        return showAnnotationsByDefault;
    }

    public void setShowAnnotationsByDefault(boolean showAnnotationsByDefault) {
        this.showAnnotationsByDefault = showAnnotationsByDefault;
    }

    public boolean isShowRPeaksByDefault() {
        return showRPeaksByDefault;
    }

    public void setShowRPeaksByDefault(boolean showRPeaksByDefault) {
        this.showRPeaksByDefault = showRPeaksByDefault;
    }

    public boolean isShowSuspiciousSegmentsByDefault() {
        return showSuspiciousSegmentsByDefault;
    }

    public void setShowSuspiciousSegmentsByDefault(boolean showSuspiciousSegmentsByDefault) {
        this.showSuspiciousSegmentsByDefault = showSuspiciousSegmentsByDefault;
    }

    public String getDefaultAnalysisMode() {
        return defaultAnalysisMode;
    }

    public void setDefaultAnalysisMode(String defaultAnalysisMode) {
        this.defaultAnalysisMode = defaultAnalysisMode;
    }
}
