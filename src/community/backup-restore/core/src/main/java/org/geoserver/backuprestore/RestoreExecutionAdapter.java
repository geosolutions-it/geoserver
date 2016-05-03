/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import java.util.zip.ZipInputStream;

import org.geoserver.catalog.Catalog;
import org.springframework.batch.core.JobExecution;

/**
 * Wraps a Spring Batch Restore {@link JobExecution} by adding specific {@link Backup} I/O parameters.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreExecutionAdapter extends AbstractExecutionAdapter {

    private ZipInputStream inputStream;
    
    private Catalog restoreCatalog;

    public RestoreExecutionAdapter(JobExecution jobExecution) {
        super(jobExecution);
    }

    /**
     * @return the inputStream
     */
    public ZipInputStream getInputStream() {
        return inputStream;
    }

    /**
     * @param inputStream the inputStream to set
     */
    public void setInputStream(ZipInputStream inputStream) {
        this.inputStream = inputStream;
    }

    /**
     * @return the restoreCatalog
     */
    public Catalog getRestoreCatalog() {
        return restoreCatalog;
    }

    /**
     * @param restoreCatalog the restoreCatalog to set
     */
    public void setRestoreCatalog(Catalog catalog) {
        this.restoreCatalog = catalog;
    }

}
