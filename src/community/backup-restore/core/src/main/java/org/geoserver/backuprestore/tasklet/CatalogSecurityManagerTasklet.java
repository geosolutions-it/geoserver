/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.util.Arrays;

import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;

import com.thoughtworks.xstream.XStream;

/**
 * TODO
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogSecurityManagerTasklet implements Tasklet, ApplicationContextAware, InitializingBean {

    public static final String SECURITY_FOLDER_NAME = "security";

    private Backup backupFacade;

    private Catalog catalog;

    private XStreamPersister xstream;

    private XStream xp;
    
    private boolean isNew;

    private AbstractExecutionAdapter currentJobExecution;

    private boolean dryRun;

    private boolean bestEffort;

    private ApplicationContext applicationContext;

    private XStreamPersisterFactory xStreamPersisterFactory;
    
    public CatalogSecurityManagerTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        this.backupFacade = backupFacade;
        this.xStreamPersisterFactory = xStreamPersisterFactory;
    }

    @SuppressWarnings("unused")
    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext)
            throws Exception {

        // Accordingly to the running execution type (Backup or Restore) we
        // need to validate resources against the official GeoServer Catalog (Backup)
        // or the temporary one (Restore).
        //
        // For restore operations the order matters.
        JobExecution jobExecution = chunkContext.getStepContext().getStepExecution()
                .getJobExecution();
        this.xstream = xStreamPersisterFactory.createXMLPersister();
        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.currentJobExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.catalog = backupFacade.getRestoreExecutions().get(jobExecution.getId()).getRestoreCatalog();
            this.isNew = true;
        } else {
            this.currentJobExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
            this.catalog = backupFacade.getCatalog();
            this.xstream.setExcludeIds();
            this.isNew = false;
        }

        Assert.notNull(catalog, "catalog must be set");

        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();

        Assert.notNull(this.xp, "xStream persister should not be NULL");
        
        JobParameters jobParameters = this.currentJobExecution.getJobParameters();
        
        this.dryRun = Boolean.parseBoolean(jobParameters.getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        this.bestEffort = Boolean.parseBoolean(jobParameters.getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        final GeoServer geoserver = backupFacade.getGeoServer();
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();
        
        // GeoServer Security Folder
        final Resource security = dd.getSecurity("/");
        
        if (!isNew) {
            final String outputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_OUTPUT_FILE_PATH);
            Resource targetBackupFolder = Resources.fromURL(outputFolderURL);
            
            // Copy the Security files into the destination folder
            Resources.copy(security, BackupUtils.dir(targetBackupFolder, SECURITY_FOLDER_NAME));
            
            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = 
                    new GeoServerSecurityManager(new GeoServerDataDirectory(targetBackupFolder.dir()));
            try {
                testGssm.setApplicationContext(applicationContext);
                testGssm.reload();
                
                // TODO: Perform validation tests here using "testGssm"
                
                // TODO: Save warnings and validation issues on the JobContext
            } catch (Exception e) {
                if(!bestEffort) {
                    currentJobExecution.addFailureExceptions(Arrays.asList(e));
                    throw new UnexpectedJobExecutionException("Exception occurred while storing GeoServer security settings!", e);
                } else {
                    currentJobExecution.addWarningExceptions(Arrays.asList(e));
                }
            } finally {
                testGssm.destroy();
            }
        } else {
            /**
             * Create a new GeoServerSecurityManager instance using the INPUT DATA DIR.
             * 
             * Try to load the configuration from there and if everything is ok:
             * 1. Replace the security folders
             * 2. Destroy and reload the appContext GeoServerSecurityManager
             * 3. Issue SecurityManagerListener extensions handlePostChanged(...)
             */
            final String inputFolderURL = jobExecution.getJobParameters().getString(Backup.PARAM_INPUT_FILE_PATH);
            Resource sourceRestoreFolder = Resources.fromURL(inputFolderURL);
            
            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = 
                    new GeoServerSecurityManager(new GeoServerDataDirectory(sourceRestoreFolder.dir()));
            try {
                testGssm.setApplicationContext(applicationContext);
                testGssm.reload();
                
                // TODO: Perform validation tests here using "testGssm"
                
                // TODO: Save warnings and validation issues on the JobContext
            } catch (Exception e) {
                if(!bestEffort) {
                    currentJobExecution.addFailureExceptions(Arrays.asList(e));
                    throw new UnexpectedJobExecutionException("Exception occurred while storing GeoServer security settings!", e);
                } else {
                    currentJobExecution.addWarningExceptions(Arrays.asList(e));
                }
            } finally {
                testGssm.destroy();
            }

            // Do this *ONLY* when DRY-RUN-MODE == OFF
            if (!dryRun) {
                // Copy the Security files into the destination folder
                // TODO: Purge datadir option
                Files.delete(security.dir());
                Resources.copy(BackupUtils.dir(sourceRestoreFolder, SECURITY_FOLDER_NAME), security);
    
                // Reload Security Context
                GeoServerSecurityManager securityContext = GeoServerExtensions.bean(GeoServerSecurityManager.class);
                securityContext.reload();
                
                for (SecurityManagerListener listener : GeoServerExtensions.extensions(SecurityManagerListener.class)) {
                    listener.handlePostChanged(securityContext);
                }
            }
        }
        
        return RepeatStatus.FINISHED;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(backupFacade, "backupFacade must be set");
        Assert.notNull(xStreamPersisterFactory, "xstream must be set");
    }
}
