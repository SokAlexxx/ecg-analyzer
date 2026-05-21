package com.ecganalyzer.model;

public class AppState {

    private ApplicationView currentView;

    public AppState() {
        this.currentView = ApplicationView.DASHBOARD;
    }

    public ApplicationView getCurrentView() {
        return currentView;
    }

    public void setCurrentView(ApplicationView currentView) {
        this.currentView = currentView;
    }
}