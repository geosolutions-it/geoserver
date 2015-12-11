/*
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.gs.mapstoreconfig.components;

import java.awt.geom.AffineTransform;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.gs.mapstoreconfig.LayerDescriptorManager;
import org.geoserver.gs.mapstoreconfig.MapstoreConfigProcess;
import org.geoserver.gs.mapstoreconfig.TemplateDirLoader;
import org.geoserver.gs.mapstoreconfig.ftl.model.DimensionsTemplateModel;
import org.geoserver.gs.mapstoreconfig.ftl.model.LayerTemplateModel;
import org.geoserver.gs.mapstoreconfig.ftl.model.LiteralDataTemplateModel;
import org.geoserver.gs.mapstoreconfig.ftl.model.MapTemplateModel;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess;
import org.geoserver.wps.gs.resource.model.Dimension;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.Resources;
import org.geoserver.wps.gs.resource.model.impl.LiteralData;
import org.geoserver.wps.gs.resource.model.impl.VectorialLayer;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.util.logging.Logging;
import org.opengis.geometry.DirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import com.thoughtworks.xstream.XStream;

/**
 * This class is responsible to manage a Layer Description manager document in XML format
 * 
 * @author DamianoG
 * 
 */
public class GeoserverXMLLayerDescriptorManager implements LayerDescriptorManager {

    private static final Logger LOGGER = Logging
            .getLogger(GeoserverXMLLayerDescriptorManager.class);

    private Catalog catalog;

    private boolean isValid;

    private boolean isValidated;

    private Resources document;

    public GeoserverXMLLayerDescriptorManager() {
    }

    public GeoserverXMLLayerDescriptorManager(Catalog catalog) {
        this.isValidated = false;
        this.isValid = false;
        this.catalog = catalog;
    }

    public void loadDocument(String xmlDocument, boolean forceReload) {
        isValidated = false;
        isValid = false;
        if (document != null && !forceReload) {
            LOGGER.warning(
                    "loadDocument method doesn't have any effect since the document has been already loaded and force reload is set to false...");
            return;
        }
        XStream xs = ResourceLoaderProcess.initialize(catalog);
        document = (Resources) xs.fromXML(xmlDocument);
    }

    public boolean validateDocument() {
        if (isValidated) {
            return isValid;
        }
        return true;
    }

    public MapTemplateModel produceModelForFTLTemplate(TemplateDirLoader templateDirLoader)
            throws IOException {

        MapTemplateModel map = new MapTemplateModel();
        List<LiteralDataTemplateModel> rawData = new ArrayList<LiteralDataTemplateModel>();
        List<LayerTemplateModel> layers = new ArrayList<LayerTemplateModel>();
        InputStream input = null;
        try {
            File dir = templateDirLoader.getTemplateDir();
            Properties prop = new Properties();
            // Load From the GeoserverDataDir (the template directory) a file with the Default values to put in the configuration
            // WARNING don't confuse these default values with the default WPS process input parameters
            input = new FileInputStream(
                    new File(dir, MapstoreConfigProcess.DEFAULT_PROPERTIES_NAME));
            prop.load(input);

            // map meta-model
            map.setProjection(prop.getProperty("mapProj"));
            map.setUnits(prop.getProperty("mapUnits"));
            map.setZoom(Integer.parseInt(prop.getProperty("mapZoom")));

            if (prop.containsKey("mapCenterX") && prop.containsKey("mapCenterY")) {
                map.setCenterX(Double.parseDouble(prop.getProperty("mapCenterX")));
                map.setCenterY(Double.parseDouble(prop.getProperty("mapCenterY")));
            }

            if (prop.containsKey("maxExtentMinX") && prop.containsKey("maxExtentMinY")
                    && prop.containsKey("maxExtentMaxX") && prop.containsKey("maxExtentMaxY")) {
                map.setMaxExtentMinX(Double.parseDouble(prop.getProperty("maxExtentMinX")));
                map.setMaxExtentMinY(Double.parseDouble(prop.getProperty("maxExtentMinY")));
                map.setMaxExtentMaxX(Double.parseDouble(prop.getProperty("maxExtentMaxX")));
                map.setMaxExtentMaxY(Double.parseDouble(prop.getProperty("maxExtentMaxY")));
                // map.setExtentMinX(Double.parseDouble(prop.getProperty("maxExtentMinX")));
                // map.setExtentMinY(Double.parseDouble(prop.getProperty("maxExtentMinY")));
                // map.setExtentMaxX(Double.parseDouble(prop.getProperty("maxExtentMaxX")));
                // map.setExtentMaxY(Double.parseDouble(prop.getProperty("maxExtentMaxY")));
            }

            if (prop.containsKey("extentMinX") && prop.containsKey("extentMinY")
                    && prop.containsKey("extentMaxX") && prop.containsKey("extentMaxY")) {
                map.setExtentMinX(Double.parseDouble(prop.getProperty("extentMinX")));
                map.setExtentMinY(Double.parseDouble(prop.getProperty("extentMinY")));
                map.setExtentMaxX(Double.parseDouble(prop.getProperty("extentMaxX")));
                map.setExtentMaxY(Double.parseDouble(prop.getProperty("extentMaxY")));
            }

            // layers meta-model
            for (Resource r : document.getResources()) {
                if (r instanceof VectorialLayer) {
                    LayerTemplateModel ltm = mapVectorLayer(r, prop, map);
                    layers.add(ltm);
                }

                if (r instanceof LiteralData) {
                    LiteralDataTemplateModel raw = mapLiteralData(r, prop, map);
                    rawData.add(raw);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }

        map.setRawData(rawData);
        map.setLayers(layers);

        return map;
    }

    /**
     * 
     */
    public String mimeFormatHandled() {
        return "application/xml";
    }

    /**
     * 
     * @param r
     * @param prop
     * @param map
     * @return
     */
    protected LayerTemplateModel mapVectorLayer(Resource r, Properties prop, MapTemplateModel map) {
        VectorialLayer vl = (VectorialLayer) r;
        LayerTemplateModel layerValues = new LayerTemplateModel();
        DimensionsTemplateModel timeDimensions = new DimensionsTemplateModel();
        layerValues.setTimeDimensions(timeDimensions);

        layerValues.setFormat(StringUtils.defaultString(prop.getProperty("format")));
        layerValues.setFixed(StringUtils.defaultString(prop.getProperty("fixed")));
        layerValues.setGroup(StringUtils.defaultString(prop.getProperty("group")));
        layerValues.setName(StringUtils.defaultString(vl.getWorkspace() + ":" + vl.getName(),
                prop.getProperty("name")));
        layerValues.setOpacity(StringUtils.defaultString(prop.getProperty("opacity")));
        layerValues.setSelected(StringUtils.defaultString(prop.getProperty("selected")));
        layerValues.setStyles(StringUtils.defaultString(vl.getDefaultStyle().get("name"),
                prop.getProperty("styles")));
        layerValues.setSource(StringUtils.defaultString(prop.getProperty("source")));
        layerValues.setTitle(StringUtils.defaultString(vl.getTitle(), prop.getProperty("title")));
        layerValues.setVisibility(StringUtils.defaultString(prop.getProperty("visibility")));
        layerValues.setTransparent(StringUtils.defaultString(prop.getProperty("transparent")));
        layerValues.setQueryable(StringUtils.defaultString(prop.getProperty("queryable")));

        // These values came directly from the process input parameters

        decorateCoordinates(prop, vl.latLonBoundingBox(), vl.getDimensions(), map);

        timeDimensions.setCurrent(StringUtils.defaultString(prop.getProperty("current")));
        timeDimensions.setDefaultVal(StringUtils.defaultString(prop.getProperty("defaultVal")));
        timeDimensions.setMultipleVal(StringUtils.defaultString(prop.getProperty("multipleVal")));
        timeDimensions.setName(StringUtils.defaultString(prop.getProperty("name")));
        timeDimensions.setNearestVal(StringUtils.defaultString(prop.getProperty("nearestVal")));
        timeDimensions.setUnits(StringUtils.defaultString(prop.getProperty("units")));
        timeDimensions.setUnitsymbol(StringUtils.defaultString(prop.getProperty("unitsymbol")));

        timeDimensions.setMinLimit(StringUtils.defaultString(prop.getProperty("minLimit")));
        timeDimensions.setMaxLimit(StringUtils.defaultString(prop.getProperty("maxLimit")));
        timeDimensions.setMinX(StringUtils.defaultString(prop.getProperty("minX")));
        timeDimensions.setMinY(StringUtils.defaultString(prop.getProperty("minY")));
        timeDimensions.setMaxX(StringUtils.defaultString(prop.getProperty("maxX")));
        timeDimensions.setMaxY(StringUtils.defaultString(prop.getProperty("maxY")));

        return layerValues;
    }

    /**
     * 
     * @param r
     * @param prop
     * @param map
     * @return
     */
    protected LiteralDataTemplateModel mapLiteralData(Resource r, Properties prop,
            MapTemplateModel map) {
        LiteralData raw = (LiteralData) r;
        LiteralDataTemplateModel rawData = new LiteralDataTemplateModel();

        rawData.setName(raw.getName());
        rawData.setText(raw.getText());

        return rawData;
    }

    /**
     * 
     * @param prop
     * @param referencedEnvelope
     * @param dimensions
     * @param map
     */
    protected static void decorateCoordinates(Properties prop,
            ReferencedEnvelope referencedEnvelope, List<Dimension> dimensions,
            MapTemplateModel map) {

        MathTransform tx = new AffineTransform2D(new AffineTransform());
        try {
            CoordinateReferenceSystem mapProjection = CRS.decode(map.getProjection(), true);
            CoordinateReferenceSystem layerCRS = referencedEnvelope.getCoordinateReferenceSystem();

            if (!CRS.equalsIgnoreMetadata(mapProjection, layerCRS)) {
                tx = CRS.findMathTransform(layerCRS, mapProjection, true);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Could not decode Map projection.", e);
        }

        // Temporal Limits
        if (dimensions != null) {
            for (Dimension dim : dimensions) {
                if (dim.getName().equalsIgnoreCase("time")) {

                    // Range
                    final String minTime = dim.getMin();
                    final String maxTime = dim.getMax();
                    prop.setProperty("minLimit",
                            (minTime != null) ? minTime : prop.getProperty("minLimit"));
                    prop.setProperty("maxLimit",
                            (maxTime != null) ? maxTime : prop.getProperty("maxLimit"));

                    // Dimension Info
                    final DimensionInfo dimInfo = dim.getDimensionInfo();
                    if (dim.getDimensionInfo() != null) {
                        // prop.setProperty("name", dimInfo.getAttribute());
                        prop.setProperty("name", dim.getName());
                        prop.setProperty("units", dimInfo.getUnits());
                        prop.setProperty("unitsymbol", dimInfo.getUnitSymbol());
                        prop.setProperty("presentation", String.valueOf(dimInfo.getPresentation()));

                        if (dimInfo.getResolution() != null)
                            prop.setProperty("resolution", String.valueOf(dimInfo.getResolution()));

                        if (dimInfo.getDefaultValue() != null
                                && dimInfo.getDefaultValue().getReferenceValue() != null)
                            prop.setProperty("defaultVal",
                                    dimInfo.getDefaultValue().getReferenceValue());
                    }
                }
            }
        }

        // Spatial Limits
        String minX = null;
        boolean centerFlag = (map.getCenterX() == null || map.getCenterY() == null);
        boolean maxExtentFlag = (map.getMaxExtentMinX() == null || map.getMaxExtentMinY() == null
                || map.getMaxExtentMaxX() == null || map.getMaxExtentMaxY() == null);
        boolean extentFlag = (map.getExtentMinX() == null || map.getExtentMinY() == null
                || map.getExtentMaxX() == null || map.getExtentMaxY() == null);
        try {
            if (referencedEnvelope != null) {
                minX = Double.toString(referencedEnvelope.getLowerCorner().getOrdinate(0));
                prop.setProperty("minX", minX);

                if (map.getMaxExtentMinX() == null) {
                    map.setMaxExtentMinX(referencedEnvelope.getLowerCorner().getOrdinate(0));
                }

                if (map.getExtentMinX() == null) {
                    map.setExtentMinX(referencedEnvelope.getLowerCorner().getOrdinate(0));
                }
            }
        } catch (Exception e) {
            minX = null;
        }
        String minY = null;
        try {
            if (referencedEnvelope != null) {
                minY = Double.toString(referencedEnvelope.getLowerCorner().getOrdinate(1));
                prop.setProperty("minY", minY);

                if (map.getMaxExtentMinY() == null) {
                    map.setMaxExtentMinY(referencedEnvelope.getLowerCorner().getOrdinate(1));
                }

                if (map.getExtentMinY() == null) {
                    map.setExtentMinY(referencedEnvelope.getLowerCorner().getOrdinate(1));
                }
            }
        } catch (Exception e) {
            minY = null;
        }
        String maxX = null;
        try {
            if (referencedEnvelope != null) {
                maxX = Double.toString(referencedEnvelope.getUpperCorner().getOrdinate(0));
                prop.setProperty("maxX", maxX);

                if (map.getMaxExtentMaxX() == null) {
                    map.setMaxExtentMaxX(referencedEnvelope.getUpperCorner().getOrdinate(0));
                }

                if (map.getExtentMaxX() == null) {
                    map.setExtentMaxX(referencedEnvelope.getUpperCorner().getOrdinate(0));
                }
            }
        } catch (Exception e) {
            maxX = null;
        }
        String maxY = null;
        try {
            if (referencedEnvelope != null) {
                maxY = Double.toString(referencedEnvelope.getUpperCorner().getOrdinate(1));
                prop.setProperty("maxY", maxY);

                if (map.getMaxExtentMaxY() == null) {
                    map.setMaxExtentMaxY(referencedEnvelope.getUpperCorner().getOrdinate(1));
                }

                if (map.getExtentMaxY() == null) {
                    map.setExtentMaxY(referencedEnvelope.getUpperCorner().getOrdinate(1));
                }
            }
        } catch (Exception e) {
            maxY = null;
        }

        if (map.getCenterX() == null) {
            if (map.getExtentMinX() != null && map.getExtentMaxX() != null) {
                map.setCenterX(
                        map.getExtentMinX() + (map.getExtentMaxX() - map.getExtentMinX()) / 2.0);
            } else if (map.getMaxExtentMinX() != null && map.getMaxExtentMaxX() != null) {
                map.setCenterX(map.getMaxExtentMinX()
                        + (map.getMaxExtentMaxX() - map.getMaxExtentMinX()) / 2.0);
            }
        }

        if (map.getCenterY() == null) {
            if (map.getExtentMinY() != null && map.getExtentMaxY() != null) {
                map.setCenterY(
                        map.getExtentMinY() + (map.getExtentMaxY() - map.getExtentMinY()) / 2.0);
            } else if (map.getMaxExtentMinY() != null && map.getMaxExtentMaxY() != null) {
                map.setCenterY(map.getMaxExtentMinY()
                        + (map.getMaxExtentMaxY() - map.getMaxExtentMinY()) / 2.0);
            }
        }

        // Fix coordinates
        if (!tx.isIdentity()) {
            try {
                // Map Center
                if (centerFlag) {
                    DirectPosition center = new DirectPosition2D(map.getCenterX(),
                            map.getCenterY());
                    tx.transform(center, center);
                    map.setCenterX(center.getOrdinate(0));
                    map.setCenterY(center.getOrdinate(1));
                }

                // Map Extents
                if (extentFlag) {
                    DirectPosition ll = new DirectPosition2D(map.getExtentMinX(),
                            map.getExtentMinY());
                    tx.transform(ll, ll);
                    map.setExtentMinX(ll.getOrdinate(0));
                    map.setExtentMinY(ll.getOrdinate(1));

                    DirectPosition ur = new DirectPosition2D(map.getExtentMaxX(),
                            map.getExtentMaxY());
                    tx.transform(ur, ur);
                    map.setExtentMaxX(ur.getOrdinate(0));
                    map.setExtentMaxY(ur.getOrdinate(1));
                }

                if (maxExtentFlag) {
                    DirectPosition maxLl = new DirectPosition2D(map.getMaxExtentMinX(),
                            map.getMaxExtentMinY());
                    tx.transform(maxLl, maxLl);
                    map.setMaxExtentMinX(maxLl.getOrdinate(0));
                    map.setMaxExtentMinY(maxLl.getOrdinate(1));

                    DirectPosition maxUr = new DirectPosition2D(map.getMaxExtentMaxX(),
                            map.getMaxExtentMaxY());
                    tx.transform(maxUr, maxUr);
                    map.setMaxExtentMaxX(maxUr.getOrdinate(0));
                    map.setMaxExtentMaxY(maxUr.getOrdinate(1));
                }
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Could not reproject coordinates to the Map Projection!",
                        e);
            }
        }
    }
}
