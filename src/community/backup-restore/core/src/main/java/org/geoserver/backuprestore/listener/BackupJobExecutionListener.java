/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.util.Set;
import java.util.logging.Logger;

import org.geoserver.backuprestore.Backup;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
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

    private Backup backupFacade;

    public BackupJobExecutionListener(Backup backupFacade) {
        this.backupFacade = backupFacade;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // TODO Auto-generated method stub
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        try {
            Set<Long> runningExecutions = backupFacade.getJobOperator().getRunningExecutions(Backup.BACKUP_JOB_NAME);
            LOGGER.fine("Running Executions IDs : " + runningExecutions);

            Long executionId = runningExecutions.iterator().next();
            if (jobExecution.getStatus() == BatchStatus.STOPPED) {
                backupFacade.getJobOperator().restart(executionId);
            } else {
                // TODO 
                LOGGER.fine("Executions Step Summaries : " + backupFacade.getJobOperator().getStepExecutionSummaries(executionId));
                LOGGER.fine("Executions Parameters : " + backupFacade.getJobOperator().getParameters(executionId));
                LOGGER.fine("Executions Summary : " + backupFacade.getJobOperator().getSummary(executionId));

                LOGGER.fine("Exit Status : " + backupFacade.getBackupExecutions().get(executionId).getStatus());
                LOGGER.fine("Exit Failures : " + backupFacade.getBackupExecutions().get(executionId).getAllFailureExceptions());
            }
        } catch (NoSuchJobException e) {
            e.printStackTrace();
        } catch (NoSuchJobExecutionException e) {
            e.printStackTrace();
        } catch (JobInstanceAlreadyCompleteException e) {
            e.printStackTrace();
        } catch (JobRestartException e) {
            e.printStackTrace();
        } catch (JobParametersInvalidException e) {
            e.printStackTrace();
        } finally {
            // TODO UNLOCK Configuration
        }
    }
}
