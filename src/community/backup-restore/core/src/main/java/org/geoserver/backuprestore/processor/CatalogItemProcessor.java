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
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.ValidationResult;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
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

    private boolean isNew;

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
            this.isNew = true;
        } else {
            this.catalog = backupFacade.getCatalog();
            this.isNew = false;
        }
    }

    @Override
    public T process(T resource) throws Exception {

        if (resource != null) {
            if (isNew) {
                // Disabling additional validators
                ((CatalogImpl) this.catalog).setExtendedValidation(false);
            }
            
            LOGGER.info("Processing resource: " + resource);

            if (resource instanceof WorkspaceInfo) {
                if (!validateWorkspace((WorkspaceInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
            } else if (resource instanceof DataStoreInfo) {
                if (!validateDataStore((DataStoreInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
                
                WorkspaceInfo ws = ((DataStoreInfo) resource).getWorkspace();
                if (this.catalog.getDefaultDataStore(ws) == null) {
                    this.catalog.setDefaultDataStore(ws, (DataStoreInfo) resource);
                }
                
            } else if (resource instanceof CoverageStoreInfo) {
                if (!validateCoverageStore((CoverageStoreInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
            }
            else if (resource instanceof ResourceInfo) {
                if (!validateResource((ResourceInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    return null;
                }
            }
            else if (resource instanceof LayerInfo) {
                try {
                    ValidationResult result = this.catalog.validate((LayerInfo) resource, isNew);
                    if (!result.isValid()) {
                     // TODO: collect warnings
                        return null;
                    }
                } catch (Exception e) {
                    // TODO: collect warnings
                    LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
                    return null;
                }
            }
            else if (resource instanceof StyleInfo) {
                try {
                    ValidationResult result = this.catalog.validate((StyleInfo) resource, isNew);
                    if (!result.isValid()) {
                     // TODO: collect warnings
                        return null;
                    }
                } catch (Exception e) {
                    // TODO: collect warnings
                    LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
                    return null;
                }
            }
            else if (resource instanceof LayerGroupInfo) {
                try {
                    ValidationResult result = this.catalog.validate((LayerGroupInfo) resource, isNew);
                    if (!result.isValid()) {
                     // TODO: collect warnings
                        return null;
                    }
                } catch (Exception e) {
                    // TODO: collect warnings
                    LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
                    return null;
                }
            }

            return resource;
        }

        return null;
    }

    /**
     * Being sure the associated {@link NamespaceInfo} exists and is available on the
     * GeoServer Catalog.
     * @param isNew 
     * 
     * @param {@link WorkspaceInfo} resource
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateWorkspace(WorkspaceInfo resource, boolean isNew) {
        final NamespaceInfo ns = this.catalog.getNamespaceByPrefix(resource.getName());
        if (ns == null) {
            return false;
        }
        
        try {
            ValidationResult result = this.catalog.validate(resource, isNew);
            if (!result.isValid()) {
             // TODO: collect warnings
                return false;
            }
        } catch (Exception e) {
            // TODO: collect warnings
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            return false;
        }
        
     // TODO: collect warnings
        return true;
    }

    /**
     * Being sure the associated {@link WorkspaceInfo} exists and is available on the
     * GeoServer Catalog.
     * 
     * Also if a default {@link DataStoreInfo} has not been defined for the current 
     * {@link WorkspaceInfo}, set this one as default.
     * @param isNew 
     * 
     * @param {@link DataStoreInfo} resource
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateDataStore(DataStoreInfo resource, boolean isNew) {
        final WorkspaceInfo ws = this.catalog.getWorkspaceByName(resource.getWorkspace().getName());
        if (ws == null) {
            return false;
        }
        
        try {
            ValidationResult result = this.catalog.validate(resource, isNew);
            if (!result.isValid()) {
             // TODO: collect warnings
                return false;
            }
        } catch (Exception e) {
            // TODO: collect warnings
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            return false;
        }
        
        resource.setWorkspace(ws);

        // TODO: collect warnings
        return true;
    }

    /**
     * Being sure the associated {@link WorkspaceInfo} exists and is available on the
     * GeoServer Catalog.
     * @param isNew 
     * 
     * @param {@link CoverageStoreInfo} resource
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateCoverageStore(CoverageStoreInfo resource, boolean isNew) {
        final WorkspaceInfo ws = this.catalog.getWorkspaceByName(resource.getWorkspace().getName());
        if (ws == null) {
            return false;
        }
        
        try {
            ValidationResult result = this.catalog.validate(resource, isNew);
            if (!result.isValid()) {
             // TODO: collect warnings
                return false;
            }
        } catch (Exception e) {
            // TODO: collect warnings
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            return false;
        }
        
        resource.setWorkspace(ws);
        return true;
    }

    /**
     * Being sure the associated {@link StoreInfo} exists and is available on the
     * GeoServer Catalog.
     * @param isNew2 
     * 
     * @param {@link ResourceInfo} resource
     * @return 
     * 
     * @return boolean indicating whether the resource is valid or not.
     */
    private boolean validateResource(ResourceInfo resource, boolean isNew) {
        try {
            final StoreInfo store = resource.getStore();
            final NamespaceInfo namespace = resource.getNamespace();
        
            if (store == null) {
                return false;
            }

            final Class storeClazz = (store instanceof DataStoreInfo ? DataStoreInfo.class : CoverageStoreInfo.class);
            final StoreInfo ds = this.catalog.getStoreByName(store.getName(), storeClazz);

            if (ds != null) {
                resource.setStore(ds);
            } else {
                return false;
            }
            
            ResourceInfo existing = catalog.getResourceByStore( store, resource.getName(), ResourceInfo.class);
            if ( existing != null && !existing.getId().equals( resource.getId() ) ) {
                //String msg = "Resource named '"+resource.getName()+"' already exists in store: '"+ store.getName()+"'";
                return false;
            }
            
            
            existing = catalog.getResourceByName( namespace, resource.getName(), ResourceInfo.class);
            if ( existing != null && !existing.getId().equals( resource.getId() ) ) {
                //String msg = "Resource named '"+resource.getName()+"' already exists in namespace: '"+ namespace.getPrefix()+"'";
                return false;
            }
            
         // TODO: collect warnings
            return true;
        } catch (Exception e) {
            e.printStackTrace();
         // TODO: collect warnings
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            return false;
        }
    }
    
}
