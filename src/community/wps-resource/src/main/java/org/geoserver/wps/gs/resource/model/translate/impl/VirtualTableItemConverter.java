/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.util.Map;

import org.geoserver.wps.gs.resource.ResourceLoaderConverter;
import org.geoserver.wps.gs.resource.model.translate.TranslateItemConverter;
import org.geotools.jdbc.VirtualTable;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * {@link ResourceLoaderConverter} extension for the marshalling/unmarshalling of {@link VirtualTable}s.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class VirtualTableItemConverter extends TranslateItemConverter {

    public VirtualTableItemConverter(String type) {
        super(type);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public boolean canConvert(Class clazz) {
        return VirtualTableItem.class.equals(clazz);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        VirtualTableItem item = (VirtualTableItem) value;

        writer.addAttribute("order", String.valueOf(item.getOrder()));
        if (item.getType() != null) {
            writer.addAttribute("class", item.getType());
        }

        if (item.getMetadata() != null) {
            writer.startNode("metadata");
            context.convertAnother(item.getMetadata());
            writer.endNode();
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        VirtualTableItem item = new VirtualTableItem();

        item.setOrder(Integer.parseInt(reader.getAttribute("order")));
        item.setType(reader.getAttribute("class"));

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String nodeName = reader.getNodeName(); // nodeName aka element's name
            Object nodeValue = reader.getValue();

            if ("metadata".equals(nodeName)) {
                item.setMetadata(
                        (Map<String, String>) context.convertAnother(nodeValue, Map.class));
            }

            reader.moveUp();
        }

        return item;
    }

}
