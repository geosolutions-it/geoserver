/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import org.geoserver.util.NearestMatchFinder;

/**
 * Represents a mapped layer in a GeoServer workspace with temporal capabilities.
 *
 * <p>This class provides methods to manage and retrieve information about a geospatial layer,
 * including its workspace, name, table name, and temporal attribute. It also supports finding the
 * nearest temporal match for a given time instant.
 */
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

    public NearestMatchFinder getNearestTimeFinder() {
        return nearestTimeFinder;
    }

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

    public void setNearestTimeFinder(NearestMatchFinder nearestTimeFinder) {
        this.nearestTimeFinder = nearestTimeFinder;
    }

    public Instant getNearest(Instant time) throws IOException {
        Date nearest = (Date) nearestTimeFinder.getNearest(Date.from(time));
        Instant result = time;
        if (nearest != null) {
            result = nearest.toInstant();
        }
        return result;
    }
}
