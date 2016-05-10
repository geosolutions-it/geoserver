/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2016, Open Source Geospatial Foundation (OSGeo)
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
package org.geoserver.backuprestore.writer;

import java.io.IOException;

import org.geoserver.backuprestore.Backup;
import org.geoserver.platform.resource.Resource;

/**
 * @author Dell
 *
 */
public interface CatalogAdditionalResourcesWriter<T> {

    public boolean canHandle(Object item);
    
    public void writeAdditionalResources(Backup backupFacade, Resource base, T item) throws IOException;
    
}
