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

import static org.geoserver.backuprestore.web.BackupRestoreWebUtils.backupFacade;
import static org.geoserver.backuprestore.web.BackupRestoreWebUtils.humanReadableByteCount;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.resource.ResourceStreamRequestHandler;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.request.resource.ContentDisposition;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.resource.FileResourceStream;
import org.apache.wicket.util.resource.IResourceStream;
import org.apache.wicket.validation.validator.RangeValidator;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.resource.Paths;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.Icon;
import org.springframework.batch.core.BatchStatus;

/**
 * @author afabiani
 *
 */
public class BackupPage extends GeoServerSecuredPage {

    public static final PackageResourceReference COMPRESS_ICON = new PackageResourceReference(
            BackupPage.class, "compress.png");
    
    static final String DETAILS_LEVEL = "expand";

    int expand = 0;
    
    File backupFile;
    
    GeoServerDialog dialog;

    private PageParameters params;

    public BackupPage(PageParameters pp) {
        this(new BackupExecutionModel(pp.get("id").toLong()), pp);
    }

    public BackupPage(BackupExecutionAdapter bkp, PageParameters pp) {
        this(new BackupExecutionModel(bkp), pp);
    }

    public BackupPage(IModel<BackupExecutionAdapter> model, PageParameters pp) {
        this.params = pp;
        
        initComponents(model);
    }

    void initComponents(final IModel<BackupExecutionAdapter> model) {
        add(new Label("id", new PropertyModel(model, "id")));
        
        BackupExecutionsProvider provider = new BackupExecutionsProvider() {
            @Override
            protected List<Property<BackupExecutionAdapter>> getProperties() {
                return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
            }
    
            @Override
            protected List<BackupExecutionAdapter> getItems() {
                return Collections.singletonList(model.getObject());
            }
        };
        
        final BackupExecutionsTable headerTable = new BackupExecutionsTable("header", provider);

        headerTable.setOutputMarkupId(true);
        headerTable.setFilterable(false);
        headerTable.setPageable(false);
        add(headerTable);
        
        final BackupExecutionAdapter bkp = model.getObject();
        boolean selectable = bkp.getStatus() != BatchStatus.COMPLETED;
        
        add(new Icon("icon", COMPRESS_ICON));
        add(new Label("title", new DataTitleModel(bkp))
          .add(new AttributeModifier("title", new DataTitleModel(bkp, false))));
        
        @SuppressWarnings("rawtypes")
        Form<?> form = new Form("form");
        add(form);
        
        try {
            if (params != null && params.getNamedKeys().contains(DETAILS_LEVEL)) {
                if (params.get(DETAILS_LEVEL).toInt() > 0) {
                    expand = params.get(DETAILS_LEVEL).toInt();
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Error parsing the 'details level' parameter: ", params.get(DETAILS_LEVEL).toString());
        }

        form.add(new SubmitLink("refresh") {
            @Override
            public void onSubmit() {
                setResponsePage(BackupPage.class, new PageParameters().add("id", params.get("id").toLong()).add(DETAILS_LEVEL, expand));
            }
        });
        
        NumberTextField<Integer> expand = new NumberTextField<Integer>("expand", new PropertyModel<Integer>(this, "expand"));
        expand.add(RangeValidator.minimum(0));
        form.add(expand);

        TextArea<String> details = new TextArea<String>("details", new BKErrorDetailsModel(bkp));
        details.setOutputMarkupId(true);
        details.setMarkupId("details");
        add(details);

        String location = bkp.getArchiveFile().path();
        if(location == null) {
            location= getGeoServerApplication().getGeoServer().getLogging().getLocation();
        }
        backupFile = new File(location);
        if (!backupFile.isAbsolute()) {
            // locate the geoserver.log file
            GeoServerDataDirectory dd = getGeoServerApplication().getBeanOfType(
                    GeoServerDataDirectory.class);
            backupFile = dd.get(Paths.convert(backupFile.getPath())).file();
        }        
        
        if (!backupFile.exists()) {
            error("Could not find the Backup Archive file: " + backupFile.getAbsolutePath());
        }
        
        add(new Link<Object>("download") {

            @Override
            public void onClick() {
                IResourceStream stream = new FileResourceStream(backupFile){
                    public String getContentType() {
                        return "application/zip";
                    }
                };
                ResourceStreamRequestHandler handler = new ResourceStreamRequestHandler(stream, backupFile.getName());
                handler.setContentDisposition(ContentDisposition.ATTACHMENT);

                RequestCycle.get().scheduleRequestHandlerAfterCurrent(handler);
            }
        });
        
        final AjaxLink cancelLink = new AjaxLink("cancel") {
            @Override
            protected void disableLink(ComponentTag tag) {
                super.disableLink(tag);
                tag.setName("a");
                tag.addBehavior(AttributeModifier.replace("class", "disabled"));
            }
    
            @Override
            public void onClick(AjaxRequestTarget target) {
                BackupExecutionAdapter bkp = model.getObject();
                //if (!bkp.isRunning()) {
                    setResponsePage(BackupDataPage.class);
                    return;
                //}

                // TODO: Try to stop the Backup Job here!
            }
    
        };
        
        cancelLink.add(new Label("text", new StringResourceModel("done", new Model("Done"))));
        add(cancelLink);
        
        add(dialog = new GeoServerDialog("dialog"));
    }
    
    @Override
    public String getAjaxIndicatorMarkupId() {
        return null;
    }

    static class DataTitleModel extends LoadableDetachableModel<String> {

        long contextId;
        boolean abbrev;
        
        DataTitleModel(BackupExecutionAdapter bkp) {
            this(bkp, true);
        }

        DataTitleModel(BackupExecutionAdapter bkp, boolean abbrev) {
            this.contextId = bkp.getId();
            this.abbrev = abbrev;
        }

        @Override
        protected String load() {
            BackupExecutionAdapter ctx = backupFacade().getBackupExecutions().get(contextId);
            String title =  ctx.getArchiveFile() != null ? ctx.getArchiveFile().path() : ctx.toString();

            if (abbrev && title.length() > 70) {
                //shorten it
                title = title.substring(0,20) + "[...]" + title.substring(title.length()-50);
            }
            
            title = title  + 
                    " [" + humanReadableByteCount(FileUtils.sizeOf(ctx.getArchiveFile().file()), false) + "]";
            
            return title;
        }
    
    }
    
    class BKErrorDetailsModel extends LoadableDetachableModel<String> {

        long contextId;
        
        public BKErrorDetailsModel(BackupExecutionAdapter bkp) {
            this.contextId = bkp.getId();
        }
        
        @Override
        protected String load() {
            BackupExecutionAdapter ctx = backupFacade().getBackupExecutions().get(contextId);
            
            StringBuilder buf = new StringBuilder();
            if (!ctx.getAllFailureExceptions().isEmpty()) {
                for (Throwable ex : ctx.getAllFailureExceptions()) {
                    ex = writeException(buf, ex, Level.SEVERE);
                }
            } else {
                buf.append("\nNO Exceptions Detected.\n");
            }
            
            if (!ctx.getAllWarningExceptions().isEmpty()) {
                for (Throwable ex : ctx.getAllWarningExceptions()) {
                    ex = writeException(buf, ex, Level.WARNING);
                }
            } else {
                buf.append("\nNO Warnings Detected.\n");
            }
            
            return buf.toString();
        }

        /**
         * @param buf
         * @param ex
         * @param severe 
         * @return
         */
        private Throwable writeException(StringBuilder buf, Throwable ex, Level level) {
            int cnt = 0;
            while (ex != null) {
                if (buf.length() > 0) {
                    buf.append('\n');
                }
                if (ex.getMessage() != null) {
                    buf.append(level).append(":");
                    buf.append(ex.getMessage());
                    cnt++;
                    
                    if (BackupPage.this.expand > 0 && BackupPage.this.expand <= cnt) {
                        StringWriter errors = new StringWriter();
                        ex.printStackTrace(new PrintWriter(errors));
                        buf.append('\n').append(errors.toString());
                    }
                }
                ex = ex.getCause();
            }
            return ex;
        }

    }
}
