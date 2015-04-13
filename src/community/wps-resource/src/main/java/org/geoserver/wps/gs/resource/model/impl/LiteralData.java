/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.impl;

import org.geoserver.wps.gs.resource.model.Resource;

/**
 * Wraps a plain CDATA text.
 * 
 * @author alessio.fabiani
 * 
 */
public class LiteralData extends Resource {

    private String text;

    /**
     * @return the text
     */
    public String getText() {
        return text;
    }

    /**
     * @param text the text to set
     */
    public void setText(String text) {
        this.text = text;
    }

    @Override
    protected boolean resourcePropertiesConsistencyCheck() {
        return true;
    }
    
}
