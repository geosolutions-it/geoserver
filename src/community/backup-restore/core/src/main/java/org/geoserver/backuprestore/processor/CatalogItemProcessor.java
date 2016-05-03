/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.processor;

import java.util.logging.Logger;

import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemProcessor;

/**
 * Concrete Spring Batch {@link ItemProcessor}.
 * 
 * Processes {@link Catalog} resource items while reading.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogItemProcessor<T> implements ItemProcessor<T, T> {

    /**
     * logger
     */
    private static final Logger LOGGER = Logging.getLogger(CatalogItemProcessor.class);

    Class<T> clazz;

    Backup backupFacade;

    private Catalog catalog;

    /**
     * Default Constructor.
     * 
     * @param clazz
     * @param backupFacade
     */
    public CatalogItemProcessor(Class<T> clazz, Backup backupFacade) {
        this.clazz = clazz;
        this.backupFacade = backupFacade;
    }

    /**
     * @return the clazz
     */
    public Class<T> getClazz() {
        return clazz;
    }
    
    @BeforeStep
    protected void retrieveInterstepData(StepExecution stepExecution) {
        // Accordingly to the running execution type (Backup or Restore) we
        // need to validate resources against the official GeoServer Catalog (Backup)
        // or the temporary one (Restore).
        // 
        // For restore operations the order matters.
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.catalog = backupFacade.getRestoreExecutions().get(jobExecution.getId())
                    .getRestoreCatalog();
        } else {
            this.catalog = backupFacade.getCatalog();
        }
    }

    @Override
    public T process(T resource) throws Exception {

        if (resource != null) {
            LOGGER.info("Processing resource: " + resource);

            if (resource instanceof WorkspaceInfo) {
                if (!validateWorkspace((WorkspaceInfo) resource)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
            } else if (resource instanceof DataStoreInfo) {
                if (!validateDataStore((DataStoreInfo) resource)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
            } else if (resource instanceof CoverageStoreInfo) {
                if (!validateCoverageStore((CoverageStoreInfo) resource)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
            }
            
//            if (item instanceof ResourceInfo) {
//                this.restoreCatalog.add((ResourceInfo) item);
//            }
//            if (item instanceof LayerInfo) {
//                this.restoreCatalog.add((LayerInfo) item);
//            }
//            if (item instanceof StyleInfo) {
//                this.restoreCatalog.add((StyleInfo) item);
//            }
//            if (item instanceof LayerGroupInfo) {
//                this.restoreCatalog.add((LayerGroupInfo) item);
//            }

            return resource;
        }

        return null;
    }

    /**
     * Being sure the associated {@link NamespaceInfo} exists and is available on the
     * GeoServer Catalog.
     * 
     * @param {@link WorkspaceInfo} resource
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateWorkspace(WorkspaceInfo resource) {
        final NamespaceInfo ns = this.catalog.getNamespaceByPrefix(resource.getName());
        if (ns != null) {
            return true;
        }

        return false;
    }

    /**
     * Being sure the associated {@link WorkspaceInfo} exists and is available on the
     * GeoServer Catalog.
     * 
     * Also if a default {@link DataStoreInfo} has not been defined for the current 
     * {@link WorkspaceInfo}, set this one as default.
     * 
     * @param {@link DataStoreInfo} resource
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateDataStore(DataStoreInfo resource) {
        final WorkspaceInfo ws = this.catalog.getWorkspaceByName(resource.getWorkspace().getName());
        if (ws != null) {
            resource.setWorkspace(ws);
            if (this.catalog.getDefaultDataStore(ws) == null) {
                this.catalog.setDefaultDataStore(ws, resource);
            }

            return true;
        }

        return false;
    }

    /**
     * Being sure the associated {@link WorkspaceInfo} exists and is available on the
     * GeoServer Catalog.
     * 
     * @param {@link CoverageStoreInfo} resource
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateCoverageStore(CoverageStoreInfo resource) {
        final WorkspaceInfo ws = this.catalog.getWorkspaceByName(resource.getWorkspace().getName());
        if (ws != null) {
            resource.setWorkspace(ws);

            return true;
        }

        return false;
    }

}
