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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * @author afabiani
 *
 */
public class BackupExecutionsProvider extends GeoServerDataProvider<BackupExecutionAdapter> {
    public static Property<BackupExecutionAdapter> ID = new BeanProperty("id", "id");
    public static Property<BackupExecutionAdapter> STATE = new BeanProperty("state", "status");
    public static Property<BackupExecutionAdapter> STARTED = new BeanProperty("started", "time");
    public static Property<BackupExecutionAdapter> OPTIONS = new BeanProperty("options", "options");
    public static Property<BackupExecutionAdapter> PROGRESS = new BeanProperty("progress", "progress");
    public static Property<BackupExecutionAdapter> ARCHIVEFILE = new BeanProperty("archiveFile", "archiveFile");

    boolean sortByUpdated = false;

    public BackupExecutionsProvider() {
        this(false);
    }

    public BackupExecutionsProvider(boolean sortByUpdated) {
        this.sortByUpdated = sortByUpdated;
    }

    @Override
    protected List<Property<BackupExecutionAdapter>> getProperties() {
        return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE);
    }
    @Override
    protected List<BackupExecutionAdapter> getItems() {
        return new ArrayList<BackupExecutionAdapter>(BackupRestoreWebUtils.backupFacade().getBackupExecutions().values());
    }

    @Override
    protected IModel<BackupExecutionAdapter> newModel(BackupExecutionAdapter object) {
        return new BackupExecutionModel((BackupExecutionAdapter) object);
    }
}