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
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;

/**
 * @author Dell
 *
 */
public class StyleInfoAdditionalResourceWriter
        implements CatalogAdditionalResourcesWriter<StyleInfo> {

    @Override
    public boolean canHandle(Object item) {
        return (item instanceof StyleInfo);
    }

    @Override
    public void writeAdditionalResources(Backup backupFacade, Resource base, StyleInfo item)
            throws IOException {
        Resource sldFile = backupFacade.getGeoServerDataDirectory().get(item, item.getFilename());

        if (sldFile != null && Resources.exists(sldFile)) {
            Resources.copy(sldFile.file(), BackupUtils.dir(base.parent(), "styles"));
        }
    }

}
