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


/**
 * @author alessio.fabiani
 * 
 */
public abstract class Resource {
    
    private String type;
    
    protected String name;

    protected boolean persistent;
    
    protected Translate translate;

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * @return the persistent
     */
    public boolean isPersistent() {
        return persistent;
    }

    /**
     * @param persistent the persistent to set
     */
    public void setPersistent(boolean persistent) {
        this.persistent = persistent;
    }

    /**
     * @return the translate
     */
    public Translate getTranslate() {
        return translate;
    }

    /**
     * @param translate the translate to set
     */
    public void setTranslate(Translate translate) {
        this.translate = translate;
    }
    
    /**
     * 
     * @return
     */
    public boolean isWellDefined() {
        boolean res = true;

        if (getType() == null || getName() == null)
            return false;

        // Check the consistency of the Translation Items
        int sourceItems = 0;
        int targetItems = 0;
        String sourceClass = null;
        String targetClass = null;

        for (TranslateItem item : getTranslate().getItems()) {
            // Check if it's SOURCE
            if (item.getType() == TranslateItem.TYPE.SOURCE) {
                sourceItems++;
                sourceClass = item.getStoreClass();
            }

            if (sourceItems > 1) {
                res = false;
                break;
            }

            // Check if it's TARGET
            if (item.getType() == TranslateItem.TYPE.TARGET) {
                targetItems++;
                targetClass = item.getStoreClass();
            }

            if (targetItems > 1) {
                res = false;
                break;
            }

        }

        // Sanity Checks
        if (sourceItems == 0)
            res = false;
        if (targetItems > 0 && !sourceClass.equals(targetClass))
            res = false;

        return res && resourcePropertiesConsistencyCheck();
    }

    /**
     * 
     * @return
     */
    protected abstract boolean resourcePropertiesConsistencyCheck();
}
