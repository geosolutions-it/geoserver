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
package org.geoserver.wps.gs.resource.model.impl;

import java.util.List;
import java.util.Map;

import org.geoserver.wps.gs.resource.ResourceLoaderConverter;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;

import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * {@link ResourceLoaderConverter} extension for the marshalling/unmarshalling of {@link VectorialLayer}s.
 * 
 * @author alessio.fabiani
 * 
 */
public class VectorialLayerConverter extends ResourceLoaderConverter {

    public VectorialLayerConverter(String type) {
        super(type);
    }

    @Override
    public boolean canConvert(Class clazz) {
        return VectorialLayer.class.equals(clazz);
    }

    @Override
    public void marshal(Object value, HierarchicalStreamWriter writer, MarshallingContext context) {
        VectorialLayer resource = (VectorialLayer) value;

        if (resource.getName() != null) {
            writer.startNode("name");
            writer.setValue(resource.getName());
            writer.endNode();
        }

        writer.startNode("persistent");
        writer.setValue(String.valueOf(resource.isPersistent()));
        writer.endNode();

        if (resource.getDefaultStyle() != null) {
            writer.startNode("defaultStyle");
            context.convertAnother(resource.getDefaultStyle());
            writer.endNode();
        }

        if (resource.getTitle() != null) {
            writer.startNode("title");
            writer.setValue(resource.getTitle());
            writer.endNode();
        }

        if (resource.getAbstract() != null) {
            writer.startNode("abstract");
            writer.setValue(resource.getAbstract());
            writer.endNode();
        }

        if (resource.getKeywords() != null) {
            writer.startNode("keywords");
            context.convertAnother(resource.getKeywords());
            writer.endNode();
        }

        if (resource.getNativeCRS() != null) {
            writer.startNode("nativeCRS");
            writer.setValue(resource.getNativeCRS());
            writer.endNode();
        }

        if (resource.getSrs() != null) {
            writer.startNode("srs");
            writer.setValue(resource.getSrs());
            writer.endNode();
        }

        if (resource.getNativeBoundingBox() != null) {
            writer.startNode("nativeBoundingBox");
            context.convertAnother(resource.getNativeBoundingBox());
            writer.endNode();
        }

        if (resource.getMetadata() != null) {
            writer.startNode("metadata");
            context.convertAnother(resource.getMetadata());
            writer.endNode();
        }

        if (resource.getTranslateContext() != null) {
            writer.startNode("translateContext");
            context.convertAnother(resource.getTranslateContext());
            writer.endNode();
        }
    }

    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        VectorialLayer resource = new VectorialLayer();

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

            if ("title".equals(nodeName)) {
                resource.setTitle((String) nodeValue);
            }

            if ("abstract".equals(nodeName)) {
                resource.setAbstract((String) nodeValue);
            }

            if ("defaultStyle".equals(nodeName)) {
                resource.setDefaultStyle((Map<String, String>) context.convertAnother(nodeValue,
                        Map.class));
            }

            if ("keywords".equals(nodeName)) {
                resource.setKeywords((List<String>) context.convertAnother(nodeValue, List.class));
            }

            if ("nativeCRS".equals(nodeName)) {
                resource.setNativeCRS((String) nodeValue);
            }

            if ("srs".equals(nodeName)) {
                resource.setSrs((String) nodeValue);
            }

            if ("nativeBoundingBox".equals(nodeName)) {
                resource.setNativeBoundingBox((Map<String, String>) context.convertAnother(
                        nodeValue, Map.class));
            }

            if ("metadata".equals(nodeName)) {
                resource.setMetadata((Map<String, String>) context.convertAnother(nodeValue,
                        Map.class));
            }

            if ("translateContext".equals(nodeName)) {
                resource.setTranslateContext((TranslateContext) context.convertAnother(nodeValue,
                        TranslateContext.class));
            }

            reader.moveUp();
        }

        return resource;
    }

}
