package org.geoserver.eumetsat.pinning.views;

import java.util.List;

public class ParsedView {
    private boolean disabled;
    private Long viewId;
    private List<String> layers;
    private String time;
    private String timeMode;

    public ParsedView(Long viewId, List<String> layers, String time, String timeMode, Boolean disabled) {
        this.viewId = viewId;
        this.layers = layers;
        this.time = time;
        this.timeMode = timeMode;
        this.disabled = disabled;
    }

    public Long getViewId() {
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

    public Boolean getDisabled() { return disabled; }
}
