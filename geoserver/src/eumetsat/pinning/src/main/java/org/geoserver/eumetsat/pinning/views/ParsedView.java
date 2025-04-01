/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
    private String drivingLayer;

    public ParsedView(
            Long viewId,
            String drivingLayer,
            List<String> layers,
            Instant time,
            String timeMode,
            Instant lastUpdate,
            Boolean disabled) {
        this.viewId = viewId;
        this.drivingLayer = drivingLayer;
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

    public String getDrivingLayer() {
        return drivingLayer;
    }

    public Boolean getDisabled() {
        return disabled;
    }
}
