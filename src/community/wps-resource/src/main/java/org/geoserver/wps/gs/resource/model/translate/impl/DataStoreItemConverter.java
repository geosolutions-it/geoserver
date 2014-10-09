/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.util.Map;

import org.geoserver.wps.gs.resource.ResourceLoaderConverter;
import org.geoserver.wps.gs.resource.model.translate.TranslateItemConverter;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * {@link ResourceLoaderConverter} extension for the marshalling/unmarshalling of {@link DataStoreItem}s.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class DataStoreItemConverter extends TranslateItemConverter {

    public DataStoreItemConverter(String type) {
        super(type);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return DataStoreItem.class.equals(clazz);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        DataStoreItem item = (DataStoreItem) value;

        writer.addAttribute("order", String.valueOf(item.getOrder()));
        if (item.getType() != null) {
            writer.addAttribute("class", item.getType());
        }

        if (item.getStore() != null) {
            writer.startNode("store");
            context.convertAnother(item.getStore());
            writer.endNode();
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        DataStoreItem item = new DataStoreItem();

        item.setOrder(Integer.parseInt(reader.getAttribute("order")));
        item.setType(reader.getAttribute("class"));

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String nodeName = reader.getNodeName(); // nodeName aka element's name
            Object nodeValue = reader.getValue();

            if ("store".equals(nodeName)) {
                item.setStore((Map<String, String>) context.convertAnother(nodeValue, Map.class));
            }

            reader.moveUp();
        }

        return item;
    }

}
