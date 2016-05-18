/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.platform.resource.Files;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;

/**
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupTest extends BackupRestoreTestSupport {
    @Test
    public void testRunSpringBatchBackupJob() throws Exception {
        BackupExecutionAdapter backupExecution = backupFacade.runBackupAsync(
                Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")), true);

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertNotNull(backupExecution);

        while (backupExecution.getStatus() != BatchStatus.COMPLETED) {
            Thread.sleep(100);

            if (backupExecution.getStatus() == BatchStatus.ABANDONED
                    || backupExecution.getStatus() == BatchStatus.FAILED
                    || backupExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertTrue(backupExecution.getStatus() == BatchStatus.COMPLETED);
    }

    @Test
    public void testTryToRunMultipleSpringBatchBackupJobs() throws Exception {
        backupFacade.runBackupAsync(
                Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")), true);
        try {
            backupFacade.runBackupAsync(
                    Files.asResource(File.createTempFile("testRunSpringBatchBackupJob", ".zip")),
                    true);
        } catch (IOException e) {
            assertEquals(e.getMessage(),
                    "Could not start a new Backup Job Execution since there are currently Running jobs.");
        }

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getBackupExecutions());
        assertTrue(!backupFacade.getBackupExecutions().isEmpty());
        assertTrue(backupFacade.getBackupRunningExecutions().size() == 1);

        BackupExecutionAdapter backupExecution = null;
        final Iterator<BackupExecutionAdapter> iterator = backupFacade.getBackupExecutions()
                .values().iterator();
        while (iterator.hasNext()) {
            backupExecution = iterator.next();
        }

        assertNotNull(backupExecution);

        while (backupExecution.getStatus() != BatchStatus.COMPLETED) {
            Thread.sleep(100);

            if (backupExecution.getStatus() == BatchStatus.ABANDONED
                    || backupExecution.getStatus() == BatchStatus.FAILED
                    || backupExecution.getStatus() == BatchStatus.UNKNOWN) {
                LOGGER.severe("backupExecution.getStatus() == " + (backupExecution.getStatus()));

                for (Throwable exception : backupExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertTrue(backupExecution.getStatus() == BatchStatus.COMPLETED);
    }

    @Test
    public void testRunSpringBatchRestoreJob() throws Exception {
        RestoreExecutionAdapter restoreExecution = backupFacade
                .runRestoreAsync(file("geoserver-full-backup.zip"));

        // Wait a bit
        Thread.sleep(100);

        assertNotNull(backupFacade.getRestoreExecutions());
        assertTrue(!backupFacade.getRestoreExecutions().isEmpty());

        assertNotNull(restoreExecution);

        Thread.sleep(100);

        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        assertNotNull(restoreCatalog);

        while (restoreExecution.getStatus() != BatchStatus.COMPLETED) {
            Thread.sleep(100);

            if (restoreExecution.getStatus() == BatchStatus.ABANDONED
                    || restoreExecution.getStatus() == BatchStatus.FAILED
                    || restoreExecution.getStatus() == BatchStatus.UNKNOWN) {

                for (Throwable exception : restoreExecution.getAllFailureExceptions()) {
                    LOGGER.log(Level.INFO, "ERROR: " + exception.getLocalizedMessage(), exception);
                    exception.printStackTrace();
                }
                break;
            }
        }

        assertTrue(restoreExecution.getStatus() == BatchStatus.COMPLETED);

        assertTrue(restoreCatalog.getWorkspaces().size() == restoreCatalog.getNamespaces().size());

        assertTrue(restoreCatalog.getDataStores().size() == 4);
        assertTrue(restoreCatalog.getResources(FeatureTypeInfo.class).size() == 14);
        assertTrue(restoreCatalog.getResources(CoverageInfo.class).size() == 4);
        assertTrue(restoreCatalog.getStyles().size() == 21);
        assertTrue(restoreCatalog.getLayers().size() == 4);
        assertTrue(restoreCatalog.getLayerGroups().size() == 1);
    }

}
