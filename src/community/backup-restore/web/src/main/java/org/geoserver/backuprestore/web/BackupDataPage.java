/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.time.Duration;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geotools.factory.Hints;
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

    WebMarkupContainer newBackupPanel;
    
    GeoServerDialog dialog;

    public BackupDataPage(PageParameters params) {
        Form form = new Form("form");
        add(form);

        newBackupPanel = new WebMarkupContainer("newBackupPanel");
        newBackupPanel.setOutputMarkupId(true);
        
        if (newBackupPanel.size() > 0) {
            newBackupPanel.remove("backupResource");
        }

        Panel p = new ResourceFilePanel("backupResource");
        newBackupPanel.add(p);
        
        form.add(newBackupPanel);
        
        form.add(new CheckBox("backupOptOverwirte", new Model<Boolean>(false)));
        form.add(new CheckBox("backupOptBestEffort", new Model<Boolean>(false)));
        form.add(statusLabel = new Label("status", new Model()).setOutputMarkupId(true));
        form.add(new AjaxSubmitLink("newBackupStart", form) {
            @Override
            protected void disableLink(ComponentTag tag) {
                super.disableLink(tag);
                tag.setName("a");
                tag.addBehavior(AttributeModifier.replace("class", "disabled"));
            }

            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.add(feedbackPanel);
            }
            
            protected void onSubmit(AjaxRequestTarget target, final Form<?> form) {
                
                //update status to indicate we are working
                statusLabel.add(AttributeModifier.replace("class", "working-link"));
                statusLabel.setDefaultModelObject("Working");
                target.add(statusLabel);
                
                setEnabled(false);
                target.add(this);
                
                final AjaxSubmitLink self = this;

                final Long jobid;
                try {
                    jobid = launchBackupExecution(form);
                } catch (Exception e) {
                    error(e);
                    LOGGER.log(Level.WARNING, "Error starting a new Backup", e);
                    return;
                } finally {
                    //update the button back to original state
                    resetButtons(form, target);

                    target.add(feedbackPanel);
                }

                this.add(new AbstractAjaxTimerBehavior(Duration.milliseconds(100)) {
                    @Override
                    protected void onTimer(AjaxRequestTarget target) {
                        Backup backupFacade = BackupRestoreWebUtils.backupFacade();
                        BackupExecutionAdapter exec = backupFacade.getBackupExecutions().get(jobid);

                       if (!exec.isRunning()) {
                           try {
                               if (exec.getAllFailureExceptions() != null && 
                                       !exec.getAllFailureExceptions().isEmpty()) {
                                   error(exec.getAllFailureExceptions().get(0));
                               }
                               else if (exec.isStopping()) {
                                   //do nothing
                               }
                               else {
                                   PageParameters pp = new PageParameters();
                                   pp.add("id", exec.getId());

                                   setResponsePage(BackupPage.class, pp);
                               }
                           }
                           catch(Exception e) {
                               error(e);
                               LOGGER.log(Level.WARNING, "", e);
                           }
                           finally {
                               stop(null);
                               
                               //update the button back to original state
                               resetButtons(form, target);

                               target.add(feedbackPanel);
                           }
                           return;
                       }

                       String msg = exec != null ? exec.getStatus().toString() : "Working";

                       statusLabel.setDefaultModelObject(msg);
                       target.add(statusLabel);
                   };
                   
                @Override
                   public boolean canCallListenerInterface(Component component, Method method) {
                       if(self.equals(component) && 
                               method.getDeclaringClass().equals(org.apache.wicket.behavior.IBehaviorListener.class) &&
                               method.getName().equals("onRequest")){
                           return true;
                       }
                       return super.canCallListenerInterface(component, method);
                   }
                });
            }

            private Long launchBackupExecution(Form<?> form) throws Exception {
                ResourceFilePanel panel = (ResourceFilePanel) newBackupPanel.get("backupResource");
                Resource archiveFile = null;
                try {
                    archiveFile = panel.getResource();
                } catch (NullPointerException e) {
                    throw new Exception("Backup Archive File is Mandatory!");
                }
                
                if (archiveFile == null || archiveFile.getType() == Type.DIRECTORY) {
                    throw new Exception("Backup Archive File is Mandatory and should not be a Directory!");
                }
                
                if (!archiveFile.name().toLowerCase().endsWith(".zip")) {
                    throw new Exception("Backup Archive File is not a vald ZIP file path! Please add '.zip' extension.");
                }

                Hints hints = new Hints(new HashMap(2));
                
                Boolean backupOptOverwirte = ((CheckBox) form.get("backupOptOverwirte")).getModelObject();
                Boolean backupOptBestEffort = ((CheckBox) form.get("backupOptBestEffort")).getModelObject();

                if (backupOptBestEffort) {
                    hints.add(new Hints(new Hints.OptionKey(Backup.PARAM_BEST_EFFORT_MODE), Backup.PARAM_BEST_EFFORT_MODE));
                }
                
                Backup backupFacade = BackupRestoreWebUtils.backupFacade();
                
                return backupFacade.runBackupAsync(archiveFile, backupOptOverwirte, hints).getId();
            }
        });
        
        backupExecutionsTable = new BackupExecutionsTable("backups", new BackupExecutionsProvider(true) {
            @Override
            protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<BackupExecutionAdapter>> getProperties() {
                return Arrays.asList(ID, STATE, STARTED, PROGRESS, ARCHIVEFILE, OPTIONS);
            }
        }, true) {
            protected void onSelectionUpdate(AjaxRequestTarget target) {
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

    protected void resetButtons(Form<?> form, AjaxRequestTarget target) {
        form.get("newBackupStart").setEnabled(true);
        statusLabel.setDefaultModelObject("");
        statusLabel.add(AttributeModifier.replace("class", ""));
        
        target.add(form.get("newBackupStart"));
        target.add(form.get("status"));
    }
}
