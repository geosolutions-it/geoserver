package org.geoserver.eumetsat.pinning;

public class MappedLayer {

        private String workspace;
        private String layerName;
        private String temporalAttribute;
        private String tableName;

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

    public String getTemporalAttribute() {
        return temporalAttribute;
    }

    public String getTableName() { return tableName;}

    public String getGeoServerLayerIdentifier() {
        return (workspace == null || workspace.isEmpty()) ? layerName : workspace + ":" + layerName;
    }

    @Override
    public String toString() {
        return "MappedLayer{" +
                "workspace='" + workspace + '\'' +
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
