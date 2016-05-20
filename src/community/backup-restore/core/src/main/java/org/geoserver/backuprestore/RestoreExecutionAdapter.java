/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore;

import org.geoserver.catalog.Catalog;
import org.geoserver.platform.resource.Resource;
import org.springframework.batch.core.JobExecution;

/**
 * Wraps a Spring Batch Restore {@link JobExecution} by adding specific {@link Backup} I/O parameters.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RestoreExecutionAdapter extends AbstractExecutionAdapter {

    private Catalog restoreCatalog;
    
    private Resource archiveFile;

    public RestoreExecutionAdapter(JobExecution jobExecution, Integer totalNumberOfSteps) {
        super(jobExecution, totalNumberOfSteps);
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

    /**
     * @return the archiveFile
     */
    public Resource getArchiveFile() {
        return archiveFile;
    }

    /**
     * @param archiveFile the archiveFile to set
     */
    public void setArchiveFile(Resource archiveFile) {
        this.archiveFile = archiveFile;
    }

}
