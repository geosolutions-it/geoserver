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
package org.geoserver.wps.gs.resource.model.plugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.wps.gs.resource.model.Resource;

/**
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

}
