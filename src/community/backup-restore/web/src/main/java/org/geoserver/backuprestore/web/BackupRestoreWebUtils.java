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
package org.geoserver.backuprestore.web;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.RuntimeConfigurationType;
import org.apache.wicket.markup.ComponentTag;
import org.geoserver.backuprestore.Backup;
import org.geoserver.web.GeoServerApplication;

/**
 * @author afabiani
 *
 */
public class BackupRestoreWebUtils {

    static Backup backupFacade() {
        return GeoServerApplication.get().getBeanOfType(Backup.class);
    }

    static boolean isDevMode() {
        return RuntimeConfigurationType.DEVELOPMENT == GeoServerApplication.get().getConfigurationType();
    }

    static void disableLink(ComponentTag tag) {
        tag.setName("a");
        tag.addBehavior(AttributeModifier.replace("class", "disabled"));
    }
}