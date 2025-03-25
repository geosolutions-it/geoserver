package org.geoserver.eumetsat.pinning.views;

import java.time.Instant;
import java.util.List;

public class ParsedView {
    private boolean disabled;
    private Long viewId;
    private List<String> layers;
    private Instant time;
    private String timeMode;
    private Instant lastUpdate;

    public ParsedView(
            Long viewId, List<String> layers, Instant time, String timeMode, Instant lastUpdate, Boolean disabled) {
        this.viewId = viewId;
        this.layers = layers;
        this.time = time;
        this.timeMode = timeMode;
        this.lastUpdate = lastUpdate;
        this.disabled = disabled;
    }

    public Long getViewId() {
        return viewId;
    }

    public List<String> getLayers() {
        return layers;
    }

    public Instant getTime() {
        return time;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public String getTimeMode() {
        return timeMode;
    }

    public Boolean getDisabled() { return disabled; }
}
