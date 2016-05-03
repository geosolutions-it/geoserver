/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.util.Assert;

/**
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupTest extends BackupRestoreTestSupport {

    DataStoreInfo store;

    @Before
    public void setupStore() {
        Catalog cat = getCatalog();

        store = cat.getFactory().createDataStore();
        store.setWorkspace(cat.getDefaultWorkspace());
        store.setName("spearfish");
        store.setType("H2");

        Map params = new HashMap();
        params.put("database", getTestData().getDataDirectoryRoot().getPath()+"/spearfish");
        params.put("dbtype", "h2");
        store.getConnectionParameters().putAll(params);
        store.setEnabled(true);
        cat.add(store);
    }
    
    @After
    public void dropStore() {
        Catalog cat = getCatalog();
        CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(cat);
        store.accept(visitor);
    }
    
    @Test
    public void testRunSpringBatchBackupJob() throws Exception {
        backupFacade.runBackupAsync(null);
        
        Assert.notEmpty(backupFacade.getBackupExecutions());
        
        BackupExecutionAdapter backupExecution = backupFacade.getBackupExecutions().get(0L);
        
        Assert.notNull(backupExecution);
        
        while(backupExecution.isRunning()) {
            Thread.sleep(100);
        }
        
        Assert.isTrue(backupExecution.getStatus() == BatchStatus.COMPLETED);
    }
    
    @Test
    public void testTryToRunMultipleSpringBatchBackupJobs() throws Exception {
        backupFacade.runBackupAsync(null);
        backupFacade.runBackupAsync(null);
        backupFacade.runBackupAsync(null);
        
        Assert.notEmpty(backupFacade.getBackupExecutions());
        Assert.isTrue(backupFacade.getBackupExecutions().size() == 1);
        
        BackupExecutionAdapter backupExecution = backupFacade.getBackupExecutions().get(0L);
        
        Assert.notNull(backupExecution);
        
        while(backupExecution.isRunning()) {
            Thread.sleep(100);
        }
        
        Assert.isTrue(backupExecution.getStatus() == BatchStatus.COMPLETED);
    }
    
    @Test
    public void testRunSpringBatchRestoreJob() throws Exception {
        backupFacade.runRestoreAsync(null);
        
        Assert.notEmpty(backupFacade.getRestoreExecutions());
        
        RestoreExecutionAdapter restoreExecution = backupFacade.getRestoreExecutions().get(0L);
        
        Assert.notNull(restoreExecution);
        
        Thread.sleep(100);
        
        final Catalog restoreCatalog = restoreExecution.getRestoreCatalog();
        Assert.notNull(restoreCatalog);
        
        while(restoreExecution.isRunning()) {
            Thread.sleep(100);
        }
        
        Assert.isTrue(restoreExecution.getStatus() == BatchStatus.COMPLETED);
        Assert.isTrue(restoreCatalog.getWorkspaces().size() == restoreCatalog.getNamespaces().size());
        Assert.isTrue(restoreCatalog.getDataStores().size() == 1);
    }
}
