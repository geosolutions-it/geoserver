/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupExecutionAdapter;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 * 
 * It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution} status.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupJobExecutionListener implements JobExecutionListener {

    /**
     * logger
     */
    private static final Logger LOGGER = Logging.getLogger(BackupJobExecutionListener.class);

    public static final LockType lockType = LockType.READ;

    private Backup backupFacade;

    private BackupExecutionAdapter backupExecution;

    GeoServerConfigurationLock locker;

    public BackupJobExecutionListener(Backup backupFacade, GeoServerConfigurationLock locker) {
        this.backupFacade = backupFacade;
        this.locker = locker;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Acquire GeoServer Configuration Lock in READ mode
        locker.lock(lockType);

        this.backupExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
    }

    @SuppressWarnings("unused")
    @Override
    public void afterJob(JobExecution jobExecution) {
        boolean dryRun = Boolean.parseBoolean(
                jobExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        boolean bestEffort = Boolean.parseBoolean(
                jobExecution.getJobParameters().getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));

        try {
            final Long executionId = jobExecution.getId();

            LOGGER.fine("Running Executions IDs : " + executionId);

            if (jobExecution.getStatus() == BatchStatus.STOPPED) {
                backupFacade.getJobOperator().restart(executionId);
            } else {
                LOGGER.fine("Executions Step Summaries : "
                        + backupFacade.getJobOperator().getStepExecutionSummaries(executionId));
                LOGGER.fine("Executions Parameters : "
                        + backupFacade.getJobOperator().getParameters(executionId));
                LOGGER.fine("Executions Summary : "
                        + backupFacade.getJobOperator().getSummary(executionId));

                LOGGER.fine("Exit Status : " + backupExecution.getStatus());
                LOGGER.fine("Exit Failures : " + backupExecution.getAllFailureExceptions());

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    JobParameters jobParameters = backupExecution.getJobParameters();
                    Resource sourceFolder = Resources
                            .fromURL(jobParameters.getString(Backup.PARAM_OUTPUT_FILE_PATH));
                    BackupUtils.compressTo(sourceFolder, backupExecution.getArchiveFile());
                }
            }
            // Collect errors
        } catch (NoSuchJobException | NoSuchJobExecutionException
                | JobInstanceAlreadyCompleteException | JobRestartException
                | JobParametersInvalidException | IOException e) {
            if (!bestEffort) {
                this.backupExecution.addFailureExceptions(Arrays.asList(e));
                throw new RuntimeException(e);
            } else {
                this.backupExecution.addWarningExceptions(Arrays.asList(e));
            }
        } finally {
            // Release locks on GeoServer Configuration
            locker.unlock(lockType);
        }
    }
}
