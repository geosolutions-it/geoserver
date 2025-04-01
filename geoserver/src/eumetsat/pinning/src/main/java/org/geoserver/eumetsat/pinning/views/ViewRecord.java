/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning.views;

import java.time.Instant;
import java.util.List;

public class ViewRecord {
    private Instant timeOriginal;
    private Instant timeMain;
    private long viewId;
    private List<String> layers;
    private String drivingLayer;
    private Instant lastUpdate;

    public ViewRecord(
            long viewId,
            Instant timeOriginal,
            Instant timeMain,
            List<String> layers,
            String drivingLayer,
            Instant lastUpdate) {
        this.drivingLayer = drivingLayer;
        this.timeOriginal = timeOriginal;
        this.timeMain = timeMain;
        this.viewId = viewId;
        this.layers = layers;
        this.lastUpdate = lastUpdate;
    }

    public Instant getTimeOriginal() {
        return timeOriginal;
    }

    public Instant getTimeMain() {
        return timeMain;
    }

    public long getId() {
        return viewId;
    }

    public List<String> getLayers() {
        return layers;
    }

    public String getDrivingLayer() {
        return drivingLayer;
    }

    public Instant getLastUpdate() {
        return lastUpdate;
    }

    public void setDrivingLayer(String drivingLayer) {
        this.drivingLayer = drivingLayer;
    }

    public void setTimeOriginal(Instant timeOriginal) {
        this.timeOriginal = timeOriginal;
    }

    public void setTimeMain(Instant timeMain) {
        this.timeMain = timeMain;
    }

    public void setLastUpdate(Instant lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    @Override
    public String toString() {
        return "ViewRecord{"
                + "viewId="
                + viewId
                + ", timeOriginal="
                + timeOriginal
                + ", timeMain="
                + timeMain
                + ", layersList="
                + layers
                + ", drivingLayer="
                + drivingLayer
                + ", lastUpdate="
                + lastUpdate
                + '}';
    }
}
