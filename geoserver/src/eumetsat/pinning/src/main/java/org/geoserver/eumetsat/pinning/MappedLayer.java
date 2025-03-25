package org.geoserver.eumetsat.pinning;

import org.geoserver.util.NearestMatchFinder;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

public class MappedLayer {

    private String workspace;
    private String layerName;
    private String temporalAttribute;
    private String tableName;
    private NearestMatchFinder nearestTimeFinder;

    public MappedLayer(String workspace, String layerName) {
        this.workspace = workspace;
        this.layerName = layerName;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getLayerName() {
        return layerName;
    }

    public NearestMatchFinder getNearestTimeFinder(){ return nearestTimeFinder; }

    public String getTemporalAttribute() {
        return temporalAttribute;
    }

    public String getTableName() {
        return tableName;
    }

    public String getGeoServerLayerIdentifier() {
        return (workspace == null || workspace.isEmpty()) ? layerName : workspace + ":" + layerName;
    }

    @Override
    public String toString() {
        return "MappedLayer{"
                + "workspace='"
                + workspace
                + '\''
                + ", layerName='"
                + layerName
                + '\''
                + ", tableName='"
                + tableName
                + '\''
                + ", temporalAttribute='"
                + temporalAttribute
                + '\''
                + '}';
    }

    public void setTemporalAttribute(String temporalAttribute) {
        this.temporalAttribute = temporalAttribute;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public void setNearestTimeFinder (NearestMatchFinder nearestTimeFinder) { this.nearestTimeFinder = nearestTimeFinder;}

    public Instant getNearest(Instant time) throws IOException {
        return ((Date) nearestTimeFinder.getNearest(Date.from(time))).toInstant();
    }
}
