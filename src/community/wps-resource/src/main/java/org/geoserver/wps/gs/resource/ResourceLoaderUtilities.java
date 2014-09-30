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
package org.geoserver.wps.gs.resource;

import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.TranslateItem;

/**
 * @author alessio.fabiani
 *
 */
public class ResourceLoaderUtilities {
    
    /**
     * 
     * @param resource
     * @return
     */
    public static boolean resourceIsWellDefined(Resource resource) {
        boolean res = true;
        
        if (resource.getType() == null || resource.getName() == null) return false;
        
        // Check the consistency of the Translation Items
        int sourceItems = 0;
        int targetItems = 0;
        String sourceClass = null;
        String targetClass = null;
        
        for (TranslateItem item : resource.getTranslate().getItems()) {
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
        if (sourceItems == 0) res = false;
        if (targetItems > 0 && !sourceClass.equals(targetClass)) res = false;
        
        return res;
    }

}
