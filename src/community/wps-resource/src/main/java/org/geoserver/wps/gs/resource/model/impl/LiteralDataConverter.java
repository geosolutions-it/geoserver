/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.impl;

import org.geoserver.wps.gs.resource.ResourceLoaderConverter;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * {@link ResourceLoaderConverter} extension for the marshalling/unmarshalling of {@link LiteralData}s.
 * 
 * @author alessio.fabiani
 * 
 */
public class LiteralDataConverter extends ResourceLoaderConverter {

    public LiteralDataConverter(String type) {
        super(type);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return LiteralData.class.equals(clazz);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        LiteralData resource = (LiteralData) value;

        if (resource.getName() != null) {
            writer.startNode("name");
            writer.setValue(resource.getName());
            writer.endNode();
        }

        writer.startNode("persistent");
        writer.setValue(String.valueOf(resource.isPersistent()));
        writer.endNode();

        if (resource.getText() != null) {
            writer.startNode("text");
            writer.setValue("<[CDATA[" + resource.getText() + "]]>");
            writer.endNode();
        }

    }

    @SuppressWarnings("unchecked")
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        LiteralData resource = new LiteralData();

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String nodeName = reader.getNodeName(); // nodeName aka element's name
            Object nodeValue = reader.getValue();

            if ("name".equals(nodeName)) {
                resource.setName((String) nodeValue);
            }

            if ("persistent".equals(nodeName)) {
                resource.setPersistent(Boolean.valueOf((String) nodeValue));
            }

            if ("text".equals(nodeName)) {
                resource.setText((String) nodeValue);
            }

            reader.moveUp();
        }

        return resource;
    }

}
