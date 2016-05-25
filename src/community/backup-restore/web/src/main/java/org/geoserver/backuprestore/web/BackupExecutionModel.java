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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geotools.util.logging.Logging;

/**
 * @author afabiani
 *
 */
public class BackupExecutionModel extends LoadableDetachableModel<BackupExecutionAdapter> {

    static Logger LOGGER = Logging.getLogger(BackupExecutionModel.class);

    long id;
    
    public BackupExecutionModel(BackupExecutionAdapter bkp) {
        this(bkp.getId());
    }

    public BackupExecutionModel(long id) {
        this.id = id;
    }
    
    @Override
    protected BackupExecutionAdapter load() {
        try {
            return BackupRestoreWebUtils.backupFacade().getBackupExecutions().get(id);
        }
        catch(Exception e) {
            LOGGER.log(Level.WARNING, "Unable to load backup execution " + id, e);
            return null;
        }
    }
}
