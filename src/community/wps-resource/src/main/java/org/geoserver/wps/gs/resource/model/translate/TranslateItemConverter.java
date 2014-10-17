/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
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
