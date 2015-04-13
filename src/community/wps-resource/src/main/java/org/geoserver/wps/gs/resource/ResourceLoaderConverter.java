/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Abstract base class for the XStream Resource Converters. Each converter can be plugged into the GeoServer environment as an extension by extending
 * this base class.
 * 
 * @author alessio.fabiani
 * 
 */
public abstract class ResourceLoaderConverter implements Converter {

    /**
     * The type of the extension allows the converter to correctly recognize the resource. This must be equal to the Resource "class" attribute
     * defined into the XML.
     */
    private final String TYPE;

    public ResourceLoaderConverter(String type) {
        TYPE = type;
    }

    /**
     * @return the tYPE
     */
    public String getTYPE() {
        return TYPE;
    }

    @Override
    public abstract boolean canConvert(Class clazz);

    @Override
    public abstract void marshal(Object value, HierarchicalStreamWriter writer,
            MarshallingContext context);

    @Override
    public abstract Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context);

}
