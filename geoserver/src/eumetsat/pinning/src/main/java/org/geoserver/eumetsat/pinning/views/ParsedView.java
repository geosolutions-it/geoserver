package org.geoserver.eumetsat.pinning.views;

import java.util.List;

public class ParsedView {
    private String viewId;
    private List<String> layers;
    private String time;
    private String timeMode;

    public ParsedView(String viewId, List<String> layers, String time, String timeMode) {
        this.viewId = viewId;
        this.layers = layers;
        this.time = time;
        this.timeMode = timeMode;
    }

    public String getViewId() {
        return viewId;
    }

    public List<String> getLayers() {
        return layers;
    }

    public String getTime() {
        return time;
    }

    public String getTimeMode() {
        return timeMode;
    }
}
