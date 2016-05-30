/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.tasklet;

import java.io.IOException;
import java.util.logging.Level;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityManagerListener;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.UnexpectedJobExecutionException;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.util.Assert;

/**
 * TODO
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogSecurityManagerTasklet extends AbstractCatalogBackupRestoreTasklet {

    public static final String SECURITY_FOLDER_NAME = "security";

    public CatalogSecurityManagerTasklet(Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(backupFacade, xStreamPersisterFactory);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {

    }

    @Override
    RepeatStatus doExecute(StepContribution contribution, ChunkContext chunkContext,
            JobExecution jobExecution) throws Exception {
        final GeoServerDataDirectory dd = backupFacade.getGeoServerDataDirectory();

        // GeoServer Security Folder
        final Resource security = dd.getSecurity("/");

        if (!isNew()) {
            final String outputFolderURL = jobExecution.getJobParameters()
                    .getString(Backup.PARAM_OUTPUT_FILE_PATH);
            Resource targetBackupFolder = Resources.fromURL(outputFolderURL);

            // Copy the Security files into the destination folder
            try {
                Resources.copy(security, BackupUtils.dir(targetBackupFolder, SECURITY_FOLDER_NAME));
            } catch (IOException e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                e));
            }

            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = null;
            try {
                testGssm = new GeoServerSecurityManager(
                        new GeoServerDataDirectory(targetBackupFolder.dir()));
                testGssm.setApplicationContext(Backup.getContext());
                testGssm.reload();

                // TODO: Perform validation tests here using "testGssm"

                // TODO: Save warnings and validation issues on the JobContext
            } catch (Exception e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                e));
            } finally {
                if (testGssm != null) {
                    try {
                        testGssm.destroy();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Test GeoServerSecurityManager Destry Error!", e);
                    }
                }
            }
        } else {
            /**
             * Create a new GeoServerSecurityManager instance using the INPUT DATA DIR.
             * 
             * Try to load the configuration from there and if everything is ok: 1. Replace the security folders 2. Destroy and reload the appContext
             * GeoServerSecurityManager 3. Issue SecurityManagerListener extensions handlePostChanged(...)
             */
            final String inputFolderURL = jobExecution.getJobParameters()
                    .getString(Backup.PARAM_INPUT_FILE_PATH);
            Resource sourceRestoreFolder = Resources.fromURL(inputFolderURL);

            // Test that the security folder has been correctly saved
            GeoServerSecurityManager testGssm = null;
            try {
                testGssm = new GeoServerSecurityManager(
                        new GeoServerDataDirectory(sourceRestoreFolder.dir()));
                testGssm.setApplicationContext(Backup.getContext());
                testGssm.reload();

                // TODO: Perform validation tests here using "testGssm"

                // TODO: Save warnings and validation issues on the JobContext
            } catch (Exception e) {
                logValidationExceptions((ValidationResult) null,
                        new UnexpectedJobExecutionException(
                                "Exception occurred while storing GeoServer security and services settings!",
                                e));
            } finally {
                if (testGssm != null) {
                    try {
                        testGssm.destroy();
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Test GeoServerSecurityManager Destry Error!", e);
                    }
                }
            }

            // Do this *ONLY* when DRY-RUN-MODE == OFF
            if (!isDryRun()) {
                // Copy the Security files into the destination folder
                // TODO: Purge datadir option
                Files.delete(security.dir());
                try {
                    Resources.copy(BackupUtils.dir(sourceRestoreFolder, SECURITY_FOLDER_NAME),
                            security);
                } catch (IOException e) {
                    logValidationExceptions((ValidationResult) null,
                            new UnexpectedJobExecutionException(
                                    "Exception occurred while storing GeoServer security and services settings!",
                                    e));
                }

                // Reload Security Context
                GeoServerSecurityManager securityContext = GeoServerExtensions
                        .bean(GeoServerSecurityManager.class);
                securityContext.reload();

                for (SecurityManagerListener listener : GeoServerExtensions
                        .extensions(SecurityManagerListener.class)) {
                    listener.handlePostChanged(securityContext);
                }
            }
        }

        return RepeatStatus.FINISHED;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(backupFacade, "backupFacade must be set");
        Assert.notNull(getxStreamPersisterFactory(), "xstream must be set");
    }
}
