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

import java.util.Date;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.ocpsoft.pretty.time.PrettyTime;

/**
 * @author afabiani
 *
 */
public class BackupExecutionsTable extends GeoServerTablePanel<BackupExecutionAdapter> {

    static PrettyTime PRETTY_TIME = new PrettyTime();

    public BackupExecutionsTable(String id, BackupExecutionsProvider dataProvider) {
        super(id, dataProvider);
    }

    public BackupExecutionsTable(String id, BackupExecutionsProvider dataProvider, boolean selectable) {
        super(id, dataProvider, selectable);
    }

    @Override
    protected Component getComponentForProperty(String id, IModel itemModel, Property property) {
        if (BackupExecutionsProvider.ID == property) {
            PageParameters pp = new PageParameters();
            pp.add("id", property.getModel(itemModel).getObject());
            return new SimpleBookmarkableLink(id, BackupPage.class, property.getModel(itemModel), pp);
        }
        else if (BackupExecutionsProvider.STARTED == property) {
            Date date = (Date) property.getModel(itemModel).getObject();
            String pretty = PRETTY_TIME.format(date);
            return new Label(id, pretty);
        }
        else if (BackupExecutionsProvider.STARTED == property) {
            Date date = (Date) property.getModel(itemModel).getObject();
            String pretty = PRETTY_TIME.format(date);
            return new Label(id, pretty);
        }
        else if (BackupExecutionsProvider.ARCHIVEFILE == property) {
            String pretty = ((Resource) property.getModel(itemModel).getObject()).name();
            return new Label(id, pretty);
        }

        return new Label(id, property.getModel(itemModel));
    }

}