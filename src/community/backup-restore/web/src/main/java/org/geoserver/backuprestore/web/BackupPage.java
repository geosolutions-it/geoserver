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

import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;

/**
 * @author afabiani
 *
 */
public class BackupPage extends GeoServerSecuredPage {

    GeoServerDialog dialog;

    AtomicBoolean running = new AtomicBoolean(false);

    public BackupPage(PageParameters pp) {
        this(new BackupExecutionModel(pp.get("id").toLong()));
    }

    public BackupPage(BackupExecutionAdapter bkp) {
        this(new BackupExecutionModel(bkp));
    }

    public BackupPage(IModel<BackupExecutionAdapter> model) {
        initComponents(model);
    }

    void initComponents(final IModel<BackupExecutionAdapter> model) {
        add(new Label("id", new PropertyModel(model, "id")));
    }
}
