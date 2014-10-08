/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs.resource.model.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Wraps a GeoServer Vector type Layer XML definition and provides some properties conversion methods for the correct GeoServer Catalog configuration.
 * 
 * @author alessio.fabiani
 * 
 */
public class VectorialLayer extends Resource {

    protected String title;

    protected String abstractTxt;

    protected List<String> keywords = new ArrayList<String>();

    protected String nativeCRS;

    protected String srs;

    protected Map<String, String> defaultStyle = new HashMap<String, String>();

    protected Map<String, String> nativeBoundingBox = new HashMap<String, String>();

    protected Map<String, String> metadata = new HashMap<String, String>();

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the abstractTxt
     */
    public String getAbstract() {
        return abstractTxt;
    }

    /**
     * @param abstractTxt the abstractTxt to set
     */
    public void setAbstract(String abstractTxt) {
        this.abstractTxt = abstractTxt;
    }

    /**
     * @return the keywords
     */
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * @param keywords the keywords to set
     */
    public void setKeywords(List<String> keywords) {
        this.keywords = keywords;
    }

    /**
     * @return the nativeCRS
     */
    public String getNativeCRS() {
        return nativeCRS;
    }

    /**
     * @param nativeCRS the nativeCRS to set
     */
    public void setNativeCRS(String nativeCRS) {
        this.nativeCRS = nativeCRS;
    }

    /**
     * @return the srs
     */
    public String getSrs() {
        return srs;
    }

    /**
     * @param srs the srs to set
     */
    public void setSrs(String srs) {
        this.srs = srs;
    }

    /**
     * @return the defaultStyle
     */
    public Map<String, String> getDefaultStyle() {
        return defaultStyle;
    }

    /**
     * @param defaultStyle the defaultStyle to set
     */
    public void setDefaultStyle(Map<String, String> defaultStyle) {
        this.defaultStyle = defaultStyle;
    }

    /**
     * @return the nativeBoundingBox
     */
    public Map<String, String> getNativeBoundingBox() {
        return nativeBoundingBox;
    }

    /**
     * @param nativeBoundingBox the nativeBoundingBox to set
     */
    public void setNativeBoundingBox(Map<String, String> nativeBoundingBox) {
        this.nativeBoundingBox = nativeBoundingBox;
    }

    /**
     * @return the metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @Override
    protected boolean resourcePropertiesConsistencyCheck() {
        return true;
    }

    /**
     * 
     * @return
     */
    public ReferencedEnvelope nativeBoundingBox() {
        if (this.nativeBoundingBox != null) {
            double x1 = Double.parseDouble(this.nativeBoundingBox.get("minx"));
            double x2 = Double.parseDouble(this.nativeBoundingBox.get("maxx"));
            double y1 = Double.parseDouble(this.nativeBoundingBox.get("miny"));
            double y2 = Double.parseDouble(this.nativeBoundingBox.get("maxy"));
            CoordinateReferenceSystem crs = null;
            if (this.nativeBoundingBox.get("crs") != null) {
                try {
                    crs = CRS.decode(this.nativeBoundingBox.get("crs"));
                } catch (NoSuchAuthorityCodeException e) {
                    LOGGER.log(Level.WARNING,
                            "Exception occurred while trying to decode Native BBOX", e);
                } catch (FactoryException e) {
                    LOGGER.log(Level.WARNING,
                            "Exception occurred while trying to decode Native BBOX", e);
                }
            }
            ReferencedEnvelope bbox = new ReferencedEnvelope(x1, x2, y1, y2, crs);
            return bbox;
        }

        return null;
    }

    /**
     * 
     * @return
     */
    public CoordinateReferenceSystem nativeCRS() {
        CoordinateReferenceSystem crs = null;
        if (this.nativeBoundingBox.get("crs") != null) {
            try {
                crs = CRS.decode(this.nativeBoundingBox.get("crs"));
            } catch (NoSuchAuthorityCodeException e) {
                LOGGER.log(Level.WARNING, "Exception occurred while trying to decode Native BBOX",
                        e);
            } catch (FactoryException e) {
                LOGGER.log(Level.WARNING, "Exception occurred while trying to decode Native BBOX",
                        e);
            }
        }
        return crs;
    }

    /**
     * 
     * @param catalog
     * @return
     */
    public StyleInfo defaultStyle(Catalog catalog) {
        if (this.defaultStyle != null && this.defaultStyle.get("name") != null) {
            StyleInfo style = catalog.getStyleByName(this.defaultStyle.get("name"));

            if (style == null && this.defaultStyle.get("filename") != null) {
                style = catalog.getFactory().createStyle();
                style.setName(this.defaultStyle.get("name"));
                style.setFilename(this.defaultStyle.get("filename"));
            }

            return style;
        }
        return null;
    }

}
