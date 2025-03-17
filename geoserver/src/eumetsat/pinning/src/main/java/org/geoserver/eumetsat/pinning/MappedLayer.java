package org.geoserver.eumetsat.pinning;

import org.geoserver.catalog.Catalog;

public class MappedLayer {

        private String layerId;
        private String workspace;
        private String layerName;
        private String temporalAttribute;
        private String tableName;

    public MappedLayer(String layerId, String workspace, String layerName) {
        this.layerId = layerId;
        this.workspace = workspace;
        this.layerName = layerName;

    }

    public String getLayerId() {
        return layerId;
    }

    public String getWorkspace() {
        return workspace;
    }

    public String getLayerName() {
        return layerName;
    }

    public String getTemporalAttribute() {
        return temporalAttribute;
    }

    public String getTableName() { return tableName;}

    public String getGeoServerLayerIdentifier() {
        return workspace + ":" + layerName;
    }

    @Override
    public String toString() {
        return "MappedLayer{" +
                "layerId ='" + layerId + '\'' +
                ", workspace='" + workspace + '\'' +
                ", layerName='" + layerName + '\'' +
                ", tableName='" + tableName + '\'' +
                ", temporalAttribute='" + temporalAttribute + '\'' +
                '}';
    }

        public void setTemporalAttribute(String temporalAttribute) {
            this.temporalAttribute = temporalAttribute;
        }

        public void setTableName(String tableName) {
            this.tableName = tableName;
        }
    }
