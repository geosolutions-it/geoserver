/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.wps.gs.resource.model.translate.TranslateItemConverter;
import org.geotools.util.logging.Logging;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class TransformItemConverter extends TranslateItemConverter {

    /** */
    static Logger LOGGER = Logging.getLogger(TransformItemConverter.class);

    public TransformItemConverter(String type) {
        super(type);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return TransformItem.class.equals(clazz);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        TransformItem item = (TransformItem) value;

        writer.addAttribute("order", String.valueOf(item.getOrder()));
        if (item.getType() != null) {
            writer.addAttribute("class", item.getType());
        }

        if (item.getTransform() != null) {
            writer.startNode("transform");
            context.convertAnother(item.getTransform());
            writer.endNode();
        }

    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        TransformItem item = new TransformItem();

        item.setOrder(Integer.parseInt(reader.getAttribute("order")));
        item.setType(reader.getAttribute("class"));
        final String className = reader.getAttribute("type");

        while (reader.hasMoreChildren()) {
            reader.moveDown();

            String nodeName = reader.getNodeName(); // nodeName aka element's name
            Object nodeValue = reader.getValue();

            if ("transform".equals(nodeName)) {
                try {
                    final Class clazz = Class.forName(className);
                    item.setTransform((ImportTransform) context.convertAnother(nodeValue, clazz));
                } catch (ClassNotFoundException cause) {
                    LOGGER.log(Level.WARNING, "Could not load the Importer Transformation.", cause);
                }
            }

            reader.moveUp();
        }

        return item;
    }
}
