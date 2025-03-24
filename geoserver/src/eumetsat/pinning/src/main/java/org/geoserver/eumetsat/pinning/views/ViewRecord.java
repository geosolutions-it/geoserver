package org.geoserver.eumetsat.pinning.views;

import java.time.Instant;
import java.util.List;

public class ViewRecord {
    private Instant timeOriginal;
    private Instant timeMain;
    private long viewId;
    private List<String> layers;

    private Instant lastUpdated;

    public ViewRecord(
            Instant timeOriginal,
            Instant timeMain,
            long viewId,
            List<String> layers,
            Instant lastUpdated) {
        this.timeOriginal = timeOriginal;
        this.timeMain = timeMain;
        this.viewId = viewId;
        this.layers = layers;
        this.lastUpdated = lastUpdated;
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

    public Instant getLastUpdated() {
        return lastUpdated;
    }

    @Override
    public String toString() {
        return "ViewRecord{"
                + "timeOriginal="
                + timeOriginal
                + ", timeMain="
                + timeMain
                + ", viewId="
                + viewId
                + ", layersList="
                + layers
                + ", lastUpdated="
                + lastUpdated
                + '}';
    }
}
