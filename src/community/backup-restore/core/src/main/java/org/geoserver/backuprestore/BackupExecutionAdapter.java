/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.zip.ZipOutputStream;

import org.springframework.batch.core.JobExecution;

/**
 * Wraps a Spring Batch Backup {@link JobExecution} by adding specific {@link Backup} I/O parameters.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupExecutionAdapter extends AbstractExecutionAdapter {

    private ZipOutputStream outputStream;

    public BackupExecutionAdapter(JobExecution jobExecution) {
        super(jobExecution);
    }

    /**
     * @return the outputStream
     */
    public ZipOutputStream getOutputStream() {
        return outputStream;
    }

    /**
     * @param outputStream the outputStream to set
     */
    public void setOutputStream(ZipOutputStream outputStream) {
        this.outputStream = outputStream;
    }

}
