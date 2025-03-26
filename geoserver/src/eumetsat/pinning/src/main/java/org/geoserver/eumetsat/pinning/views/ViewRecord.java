package org.geoserver.eumetsat.pinning.views;

import java.time.Instant;
import java.util.List;

public class ViewRecord {
    private Instant timeOriginal;
    private Instant timeMain;
    private long viewId;
    private List<String> layers;
    private Instant lastUpdate;

    public ViewRecord(
            long viewId,
            Instant timeOriginal,
            Instant timeMain,
            List<String> layers,
            Instant lastUpdate) {
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

    public Instant getLastUpdate() {
        return lastUpdate;
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
                + ", lastUpdate="
                + lastUpdate
                + '}';
    }
}
