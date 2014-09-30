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
package org.geoserver.wps.gs.resource.model;

import java.util.ArrayList;
import java.util.List;

/**
 * @author alessio.fabiani
 * 
 */
public class Translate {

    private List<TranslateItem> items = new ArrayList<TranslateItem>();

    /**
     * @return the items
     */
    public List<TranslateItem> getItems() {
        return items;
    }

    /**
     * @param items the items to set
     */
    public void setItems(List<TranslateItem> items) {
        this.items = items;
    }
}
