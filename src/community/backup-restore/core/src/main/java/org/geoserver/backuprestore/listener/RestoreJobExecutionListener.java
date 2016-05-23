/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import java.util.Arrays;
import java.util.logging.Logger;

import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.GeoServerConfigurationLock.LockType;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Wrapper;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;

/**
 * Implements a Spring Batch {@link JobExecutionListener}.
 * 
 * It's used to perform operations accordingly to the {@link Backup} batch {@link JobExecution} status.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreJobExecutionListener implements JobExecutionListener {

    static Logger LOGGER = Logging.getLogger(RestoreJobExecutionListener.class);
    
    public static final LockType lockType = LockType.WRITE;

    private Backup backupFacade;

    private RestoreExecutionAdapter restoreExecution;
    
    GeoServerConfigurationLock locker;

    public RestoreJobExecutionListener(Backup backupFacade, GeoServerConfigurationLock locker) {
        this.backupFacade = backupFacade;
        this.locker = locker;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        // Acquire GeoServer Configuration Lock in WRITE mode
        locker.lock(lockType);
        
        // Prior starting the JobExecution, lets store a new empty GeoServer Catalog into the context.
        // It will be used to load the resources on a temporary in-memory configuration, which will be
        // swapped at the end of the Restore if everything goes well.
        
        this.restoreExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());
        this.restoreExecution.setRestoreCatalog(createRestoreCatalog());
    }

    private Catalog createRestoreCatalog() {
        CatalogImpl restoreCatalog = new CatalogImpl();
        Catalog gsCatalog = backupFacade.getGeoServer().getCatalog();
        if (gsCatalog instanceof Wrapper) {
            gsCatalog = ((Wrapper)gsCatalog).unwrap(Catalog.class);
        }
        
        restoreCatalog.setResourceLoader(gsCatalog.getResourceLoader());
        restoreCatalog.setResourcePool(gsCatalog.getResourcePool());
        
        for (CatalogListener listener : gsCatalog.getListeners()) {
            restoreCatalog.addListener(listener);
        }

        return restoreCatalog;
    }

    @SuppressWarnings("unused")
    @Override
    public void afterJob(JobExecution jobExecution) {
        boolean dryRun = Boolean.parseBoolean(jobExecution.getJobParameters().getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        boolean bestEffort = Boolean.parseBoolean(jobExecution.getJobParameters().getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));
        try {
            final Long executionId = jobExecution.getId();
            
            LOGGER.fine("Running Executions IDs : " + executionId);
            
            if (jobExecution.getStatus() == BatchStatus.STOPPED) {
                backupFacade.getJobOperator().restart(executionId);
            } else {
                LOGGER.fine("Executions Step Summaries : " + backupFacade.getJobOperator().getStepExecutionSummaries(executionId));
                LOGGER.fine("Executions Parameters : " + backupFacade.getJobOperator().getParameters(executionId));
                LOGGER.fine("Executions Summary : " + backupFacade.getJobOperator().getSummary(executionId));

                LOGGER.fine("Exit Status : " + restoreExecution.getStatus());
                LOGGER.fine("Exit Failures : " + restoreExecution.getAllFailureExceptions());

                if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
                    // Reload GeoServer Catalog
                    if (!dryRun) {
                        backupFacade.getGeoServer().reload();
                    }
                }
            }
         // Collect errors
        } catch (Exception e) {
            if(!bestEffort) {
                this.restoreExecution.addFailureExceptions(Arrays.asList(e));
                throw new RuntimeException(e);
            } else {
                this.restoreExecution.addWarningExceptions(Arrays.asList(e));
            }
        } finally {
            // Release locks on GeoServer Configuration
            locker.unlock(lockType);
        }
    }
}
