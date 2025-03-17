package org.geoserver.eumetsat.pinning.views;

import java.util.List;

public class ParsedView {
    private String viewId;
    private List<String> layers;
    private String lockedTime;

    public ParsedView(String viewId, List<String> layers, String lockedTime) {
        this.viewId = viewId;
        this.layers = layers;
        this.lockedTime = lockedTime;
    }

    public String getViewId() {
        return viewId;
    }

    public List<String> getLayers() {
        return layers;
    }

    public String getLockedTime() {
        return lockedTime;
    }
}
