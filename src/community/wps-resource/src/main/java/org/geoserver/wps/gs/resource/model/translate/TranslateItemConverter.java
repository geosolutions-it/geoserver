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
package org.geoserver.wps.gs.resource.model.translate;

import org.geoserver.wps.gs.resource.ResourceLoaderConverter;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * {@link ResourceLoaderConverter} extension for the marshalling/unmarshalling of {@link TranslateItem}s.
 * 
 * @author alessio.fabiani
 * 
 */
public abstract class TranslateItemConverter implements Converter {

    private final String TYPE;

    public TranslateItemConverter(String type) {
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
