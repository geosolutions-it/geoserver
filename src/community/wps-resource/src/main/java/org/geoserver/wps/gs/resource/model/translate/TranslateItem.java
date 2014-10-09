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

import java.io.IOException;
import java.util.logging.Logger;

import org.geoserver.importer.Importer;
import org.geotools.util.logging.Logging;

/**
 * The base class for an XStream translate item. Those are generic items wrapping DataStores, CoverageStore, Transformation elements of the Import
 * package and any other custom implementation which can handle part of the {@link TranlateContext} workflow.
 * 
 * A {@link TranslateItem} is a component of a state machine which runs atomic steps in a predefined order sequence. Each {@link TranslateItem} should
 * configure and manage a piece of the wrapped {@link Importer} context.
 * 
 * @author alessio.fabiani
 * 
 */
@SuppressWarnings("rawtypes")
public abstract class TranslateItem implements Comparable {

    static protected Logger LOGGER = Logging.getLogger(TranslateItem.class);

    private String type;

    private int order;

    /**
     * @return the type
     */
    public String getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * @return the order
     */
    public int getOrder() {
        return order;
    }

    /**
     * @param order the order to set
     */
    public void setOrder(int order) {
        this.order = order;
    }

    /**
     * State machine base logic
     * 
     * @param translateContext
     * @return
     * @throws IOException
     */
    public TranslateItem run(TranslateContext context) throws IOException {

        TranslateItem item = execute(context);

        return (item != null ? item : context.getNext(this));
    }

    /**
     * 
     * @param context
     * @return
     * @throws IOException
     */
    protected abstract TranslateItem execute(TranslateContext context) throws IOException;

    @Override
    public int compareTo(Object o) {
        if (o instanceof TranslateItem) {
            return Integer.compare(getOrder(), ((TranslateItem) o).getOrder());
        } else {
            return 0;
        }
    }
}
