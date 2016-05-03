/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

import com.thoughtworks.xstream.XStream;

/**
 * Primary controller/facade of the backup and restore subsystem.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class Backup implements DisposableBean, ApplicationContextAware, ApplicationListener {

    static Logger LOGGER = Logging.getLogger(Backup.class);

    public static final String PARAM_TIME = "time";

    public static final String PARAM_OUTPUT_FILE_PATH = "output.file.path";

    public static final String PARAM_INPUT_FILE_PATH = "input.file.path";

    public static final String BACKUP_JOB_NAME = "backupJob";

    public static final String RESTORE_JOB_NAME = "restoreJob";
    
    public static final String RESTORE_CATALOG_KEY = "restore.catalog";

    /** catalog */
    Catalog catalog;

    GeoServer geoServer;

    GeoServerResourceLoader rl;

    GeoServerDataDirectory dd;

    XStreamPersisterFactory xpf;
    
    JobOperator jobOperator;
    
    JobLauncher jobLauncher;

    Job backupJob;
    
    Job restoreJob;
    
    ConcurrentHashMap<Long, BackupExecutionAdapter> backupExecutions = new ConcurrentHashMap<Long, BackupExecutionAdapter>();
    
    ConcurrentHashMap<Long, RestoreExecutionAdapter> restoreExecutions = new ConcurrentHashMap<Long, RestoreExecutionAdapter>();
    
    /**
     * A static application context
     */
    static ApplicationContext context;

    public Backup(Catalog catalog, GeoServerResourceLoader rl) {
        this.catalog = catalog;
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);

        this.rl = rl;
        this.dd = new GeoServerDataDirectory(rl);
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    }

    /**
     * @return the jobOperator
     */
    public JobOperator getJobOperator() {
        return jobOperator;
    }

    /**
     * @return the jobLauncher
     */
    public JobLauncher getJobLauncher() {
        return jobLauncher;
    }

    /**
     * @return the Backup job
     */
    public Job getBackupJob() {
        return backupJob;
    }

    /**
     * @return the Restore job
     */
    public Job getRestoreJob() {
        return restoreJob;
    }

    /**
     * @return the backupExecutions
     */
    public ConcurrentHashMap<Long, BackupExecutionAdapter> getBackupExecutions() {
        return backupExecutions;
    }

    /**
     * @return the restoreExecutions
     */
    public ConcurrentHashMap<Long, RestoreExecutionAdapter> getRestoreExecutions() {
        return restoreExecutions;
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        // load the context store here to avoid circular dependency on creation
        if (event instanceof ContextLoadedEvent) {
            this.jobOperator = (JobOperator) context.getBean("jobOperator");
            this.jobLauncher = (JobLauncher) context.getBean("jobLauncherAsync");
            
            this.backupJob = (Job) context.getBean(BACKUP_JOB_NAME);
            this.restoreJob = (Job) context.getBean(RESTORE_JOB_NAME);
        }
    }

    /**
     * @return
     */
    public Set<Long> getBackupRunningExecutions() {
        Set<Long> runningExecutions;
        try {
            runningExecutions = jobOperator.getRunningExecutions(BACKUP_JOB_NAME);                    
        } catch (NoSuchJobException e) {
            runningExecutions = new HashSet<>();
        }
        return runningExecutions;
    }

    /**
     * @return
     */
    public Set<Long> getRestoreRunningExecutions() {
        Set<Long> runningExecutions;
        try {
            runningExecutions = jobOperator.getRunningExecutions(RESTORE_JOB_NAME);                    
        } catch (NoSuchJobException e) {
            runningExecutions = new HashSet<>();
        }
        return runningExecutions;
    }
    
    public Catalog getCatalog() {
        return catalog;
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    @Override
    public void destroy() throws Exception {
        // TODO Auto-generated method stub
    }

    @Override
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        Backup.context = context;
    }

    protected String getItemName(XStreamPersister xp, Class clazz) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

    /**
     * @return 
     * 
     */
    public BackupExecutionAdapter runBackupAsync(ZipOutputStream zout) {
        // TODO: Create Temp Folder Here...
        JobParameters params = new JobParametersBuilder()
                .addString(PARAM_OUTPUT_FILE_PATH, "file://F:/tmp/xml/outputs/")
                .addLong(PARAM_TIME, System.currentTimeMillis())
                .toJobParameters();

        BackupExecutionAdapter backupExecution;
        try {
            synchronized(jobOperator) {
                if(getRestoreRunningExecutions().isEmpty() && 
                        getBackupRunningExecutions().isEmpty()) {
                    backupExecution = new BackupExecutionAdapter(jobLauncher.run(backupJob, params));
                    backupExecution.setOutputStream(zout);
                    backupExecutions.put(backupExecution.getId(), backupExecution);

                    LOGGER.fine("Status : " + backupExecution.getStatus());
                    
                    return backupExecution;
                }
                
                // TODO: Else throw an Exception
            }
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            // TODO
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * @return 
     * @return 
     * 
     */
    public RestoreExecutionAdapter runRestoreAsync(ZipInputStream zin) {
        // TODO: Create Temp Folder Here...
        JobParameters params = new JobParametersBuilder()
                .addString(PARAM_INPUT_FILE_PATH, "file://F:/tmp/xml/outputs/")
                .addString(PARAM_OUTPUT_FILE_PATH, "file://F:/tmp/xml/outputs/")
                .addLong(PARAM_TIME, System.currentTimeMillis())
                .toJobParameters();

        RestoreExecutionAdapter restoreExecution;
        try {
            synchronized(jobOperator) {
                if(getRestoreRunningExecutions().isEmpty() && 
                        getBackupRunningExecutions().isEmpty()) {
                    restoreExecution = new RestoreExecutionAdapter(jobLauncher.run(restoreJob, params));
                    restoreExecution.setInputStream(zin);
                    
                    restoreExecutions.put(restoreExecution.getId(), restoreExecution);

                    LOGGER.fine("Status : " + restoreExecution.getStatus());
                    
                    return restoreExecution;
                }
                
                // TODO: Else throw an Exception
            }
        } catch (JobExecutionAlreadyRunningException | JobRestartException
                | JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
            // TODO
            e.printStackTrace();
        }
        
        return null;
    }
    
    public XStreamPersister createXStreamPersisterXML() {
        return initXStreamPersister(new XStreamPersisterFactory().createXMLPersister());
    }

    public XStreamPersister createXStreamPersisterJSON() {
        return initXStreamPersister(new XStreamPersisterFactory().createJSONPersister());
    }

    public XStreamPersister initXStreamPersister(XStreamPersister xp) {
        xp.setCatalog(catalog);
        //xp.setReferenceByName(true);
        
        XStream xs = xp.getXStream();

        //ImportContext
        xs.alias("backup", BackupExecutionAdapter.class);
        
        // security
        xs.allowTypes(new Class[] { BackupExecutionAdapter.class });
        xs.allowTypeHierarchy(Resource.class);

        return xp;
    }
}
