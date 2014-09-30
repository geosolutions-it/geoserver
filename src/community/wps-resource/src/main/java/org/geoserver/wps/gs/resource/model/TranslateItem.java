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

import java.util.Map;

/**
 * @author alessio.fabiani
 * 
 */
public class TranslateItem {
    
    public enum TYPE {
        SOURCE, TARGET
    }

    private TYPE type;
    
    private String storeClass;

    private Map<String, String> store;

    /**
     * @return the type
     */
    public TYPE getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(TYPE type) {
        this.type = type;
    }

    /**
     * @return the storeClass
     */
    public String getStoreClass() {
        return storeClass;
    }

    /**
     * @param storeClass the storeClass to set
     */
    public void setStoreClass(String storeClass) {
        this.storeClass = storeClass;
    }

    /**
     * @return the store
     */
    public Map<String, String> getStore() {
        return store;
    }

    /**
     * @param store the store to set
     */
    public void setStore(Map<String, String> store) {
        this.store = store;
    }

}
