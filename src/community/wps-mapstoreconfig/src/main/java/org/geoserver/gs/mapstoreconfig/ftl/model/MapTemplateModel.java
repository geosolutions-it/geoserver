/**
 * 
 */
package org.geoserver.gs.mapstoreconfig.ftl.model;

import java.util.List;

/**
 * @author alessio.fabiani
 * 
 */
public class MapTemplateModel {

    private Double centerX;

    private Double centerY;

    private Double maxExtentMinX;

    private Double maxExtentMinY;

    private Double maxExtentMaxX;

    private Double maxExtentMaxY;

    private Double extentMinX;

    private Double extentMinY;

    private Double extentMaxX;

    private Double extentMaxY;

    private String projection;

    private String units;

    private Integer zoom;

    private List<LiteralDataTemplateModel> rawData;

    private List<LayerTemplateModel> layers;

    private List<MetocTemplateModel> metocs;

    /**
     * @return the centerX
     */
    public Double getCenterX() {
        return centerX;
    }

    /**
     * @param centerX the centerX to set
     */
    public void setCenterX(double centerX) {
        this.centerX = centerX;
    }

    /**
     * @return the centerY
     */
    public Double getCenterY() {
        return centerY;
    }

    /**
     * @param centerY the centerY to set
     */
    public void setCenterY(double centerY) {
        this.centerY = centerY;
    }

    /**
     * @return the maxExtentMinX
     */
    public Double getMaxExtentMinX() {
        return maxExtentMinX;
    }

    /**
     * @param maxExtentMinX the maxExtentMinX to set
     */
    public void setMaxExtentMinX(double maxExtentMinX) {
        this.maxExtentMinX = maxExtentMinX;
    }

    /**
     * @return the maxExtentMinY
     */
    public Double getMaxExtentMinY() {
        return maxExtentMinY;
    }

    /**
     * @param maxExtentMinY the maxExtentMinY to set
     */
    public void setMaxExtentMinY(double maxExtentMinY) {
        this.maxExtentMinY = maxExtentMinY;
    }

    /**
     * @return the maxExtentMaxX
     */
    public Double getMaxExtentMaxX() {
        return maxExtentMaxX;
    }

    /**
     * @param maxExtentMaxX the maxExtentMaxX to set
     */
    public void setMaxExtentMaxX(double maxExtentMaxX) {
        this.maxExtentMaxX = maxExtentMaxX;
    }

    /**
     * @return the maxExtentMaxY
     */
    public Double getMaxExtentMaxY() {
        return maxExtentMaxY;
    }

    /**
     * @param maxExtentMaxY the maxExtentMaxY to set
     */
    public void setMaxExtentMaxY(double maxExtentMaxY) {
        this.maxExtentMaxY = maxExtentMaxY;
    }

    /**
     * @return the extentMinX
     */
    public Double getExtentMinX() {
        return extentMinX;
    }

    /**
     * @param extentMinX the extentMinX to set
     */
    public void setExtentMinX(double extentMinX) {
        this.extentMinX = extentMinX;
    }

    /**
     * @return the extentMinY
     */
    public Double getExtentMinY() {
        return extentMinY;
    }

    /**
     * @param extentMinY the extentMinY to set
     */
    public void setExtentMinY(double extentMinY) {
        this.extentMinY = extentMinY;
    }

    /**
     * @return the extentMaxX
     */
    public Double getExtentMaxX() {
        return extentMaxX;
    }

    /**
     * @param extentMaxX the extentMaxX to set
     */
    public void setExtentMaxX(double extentMaxX) {
        this.extentMaxX = extentMaxX;
    }

    /**
     * @return the extentMaxY
     */
    public Double getExtentMaxY() {
        return extentMaxY;
    }

    /**
     * @param extentMaxY the extentMaxY to set
     */
    public void setExtentMaxY(double extentMaxY) {
        this.extentMaxY = extentMaxY;
    }

    /**
     * @return the projection
     */
    public String getProjection() {
        return projection;
    }

    /**
     * @param projection the projection to set
     */
    public void setProjection(String projection) {
        this.projection = projection;
    }

    /**
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * @return the zoom
     */
    public int getZoom() {
        return zoom;
    }

    /**
     * @param zoom the zoom to set
     */
    public void setZoom(int zoom) {
        this.zoom = zoom;
    }

    /**
     * @return the rawData
     */
    public List<LiteralDataTemplateModel> getRawData() {
        return rawData;
    }

    /**
     * @param rawData the rawData to set
     */
    public void setRawData(List<LiteralDataTemplateModel> rawData) {
        this.rawData = rawData;
    }

    /**
     * @return the layers
     */
    public List<LayerTemplateModel> getLayers() {
        return layers;
    }

    /**
     * @param layers the layers to set
     */
    public void setLayers(List<LayerTemplateModel> layers) {
        this.layers = layers;
    }

    /**
     * @return the metocs
     */
    public List<MetocTemplateModel> getMetocs() {
        return metocs;
    }

    /**
     * @param metocs the metocs to set
     */
    public void setMetocs(List<MetocTemplateModel> metocs) {
        this.metocs = metocs;
    }

}
