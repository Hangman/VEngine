package de.pottgames.vengine.core;

public class ApplicationConfiguration {
    private boolean             debugMode           = false;
    private WindowConfiguration windowConfiguration = new WindowConfiguration();


    public boolean isDebugMode() {
        return this.debugMode;
    }


    public void setDebugMode(boolean debugMode) {
        this.debugMode = debugMode;
    }


    public WindowConfiguration getWindowConfiguration() {
        return this.windowConfiguration;
    }


    public void setWindowConfiguration(WindowConfiguration windowConfiguration) {
        this.windowConfiguration = windowConfiguration;
    }

}
