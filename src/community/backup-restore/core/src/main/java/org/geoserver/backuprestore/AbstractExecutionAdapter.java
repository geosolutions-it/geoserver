/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;

/**
 * Base Class for {@link JobExecution} wrappers.
 * Those will be used to share objects, I/O parameters and GeoServer B/R specific 
 * variables and the batch contexts.
 * 
 * {@link ConcurrentHashMap}s are populated from the {@link Backup} facade in order
 * to allow external classes to follow jobs executions and retrieve configuration,
 * parameters and statuses.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public abstract class AbstractExecutionAdapter {

    private Integer totalNumberOfSteps;

    protected JobExecution delegate;
    
    private List<String> options = Collections.synchronizedList(new ArrayList<String>());;
    
    /**
     * Default Constructor
     * 
     * @param jobExecution
     */
    public AbstractExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        this.delegate = jobExecution;
        this.totalNumberOfSteps = totalNumberOfSteps;
    }
    
    /**
     * The Unique Job Execution ID
     * 
     * @return
     */
    public Long getId() {
        return delegate.getId();
    }

    /**
     * Convenience getter for for the id of the enclosing job. Useful for DAO implementations.
     *
     * @return the id of the enclosing job
     */
    public Long getJobId() {
        return delegate.getJobId();
    }

    /**
     * The Spring Batch {@link JobParameters}
     *  
     * @return JobParameters of the enclosing job
     */
    public JobParameters getJobParameters() {
        return delegate.getJobParameters();
    }

    /**
     * The Spring Batch {@link BatchStatus}
     *  
     * @return BatchStatus of the enclosing job
     */
    public BatchStatus getStatus() {
        return delegate.getStatus();
    }

    /**
     * The Spring Batch {@link ExitStatus}
     * 
     * @return the exitCode of the enclosing job
     */
    public ExitStatus getExitStatus() {
        return delegate.getExitStatus();
    }

    /**
     * The Spring Batch {@link JobInstance}
     * 
     * @return the Job that is executing.
     */
    public JobInstance getJobInstance() {
        return delegate.getJobInstance();
    }

    /**
     * Test if this {@link JobExecution} indicates that it is running. It should be noted that this does not necessarily mean that it has been
     * persisted as such yet.
     * 
     * @return true if the end time is null
     */
    public boolean isRunning() {
        return delegate.isRunning();
    }

    /**
     * Test if this {@link JobExecution} indicates that it has been signalled to stop.
     * 
     * @return true if the status is {@link BatchStatus#STOPPING}
     */
    public boolean isStopping() {
        return delegate.isStopping();
    }

    /**
     * Return all failure causing exceptions for this JobExecution, including step executions.
     *
     * @return List&lt;Throwable&gt; containing all exceptions causing failure for this JobExecution.
     */
    public List<Throwable> getAllFailureExceptions() {
        return delegate.getAllFailureExceptions();
    }

    /**
     * Returns the total number of Job steps
     * 
     * @return the totalNumberOfSteps
     */
    public Integer getTotalNumberOfSteps() {
        return totalNumberOfSteps;
    }

    /**
     * Returns the current number of executed steps.
     * 
     * @return
     */
    public Integer getExecutedSteps() {
        return delegate.getStepExecutions().size();
    }

    /**
     * @return the options
     */
    public List<String> getOptions() {
        return options;
    }
}
