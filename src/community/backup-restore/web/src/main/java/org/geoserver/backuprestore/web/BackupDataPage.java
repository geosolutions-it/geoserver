/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.util.logging.Logging;

/**
 * First page of the backup wizard.
 * 
 * @author Andrea Aime - OpenGeo
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("serial")
public class BackupDataPage extends GeoServerSecuredPage {
    
    static Logger LOGGER = Logging.getLogger(BackupDataPage.class);

    Component statusLabel;

    BackupExecutionsTable backupExecutionsTable;

    GeoServerDialog dialog;

    public BackupDataPage(PageParameters params) {
        Form form = new Form("form");
        add(form);

        backupExecutionsTable = new BackupExecutionsTable("backups", new BackupExecutionsProvider(true) {
            @Override
            protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<BackupExecutionAdapter>> getProperties() {
                return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
            }
        }, true) {
            protected void onSelectionUpdate(AjaxRequestTarget target) {
//                removeImportLink.setEnabled(!getSelection().isEmpty());
//                target.add(removeImportLink);
            };
        };
        backupExecutionsTable.setOutputMarkupId(true);
        backupExecutionsTable.setFilterable(false);
        backupExecutionsTable.setSortable(false);
        form.add(backupExecutionsTable);

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialWidth(600);
        dialog.setInitialHeight(400);
        dialog.setMinimalHeight(150);
    }
}
