/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.processor;

import java.util.Arrays;
import java.util.logging.Logger;

import org.geoserver.backuprestore.AbstractExecutionAdapter;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.RestoreExecutionAdapter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
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
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geotools.util.logging.Logging;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.util.Assert;

import com.thoughtworks.xstream.XStream;

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
    
    protected XStreamPersister xstream;

    private XStream xp;

    private boolean isNew;

    private AbstractExecutionAdapter currentJobExecution;

    private boolean dryRun;

    private boolean bestEffort;

    private XStreamPersisterFactory xStreamPersisterFactory;
    
    /**
     * Default Constructor.
     * 
     * @param clazz
     * @param backupFacade
     */
    public CatalogItemProcessor(Class<T> clazz, Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        this.clazz = clazz;
        this.backupFacade = backupFacade;
        this.xStreamPersisterFactory = xStreamPersisterFactory;
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
        this.xstream = xStreamPersisterFactory.createXMLPersister();
        if (backupFacade.getRestoreExecutions() != null
                && !backupFacade.getRestoreExecutions().isEmpty()
                && backupFacade.getRestoreExecutions().containsKey(jobExecution.getId())) {
            this.currentJobExecution = backupFacade.getRestoreExecutions().get(jobExecution.getId());
            this.catalog = ((RestoreExecutionAdapter)currentJobExecution).getRestoreCatalog();
            this.isNew = true;
        } else {
            this.currentJobExecution = backupFacade.getBackupExecutions().get(jobExecution.getId());
            this.catalog = backupFacade.getCatalog();
            this.xstream.setExcludeIds();
            this.isNew = false;
        }
        
        Assert.notNull(this.catalog, "catalog must be set");

        this.xstream.setCatalog(this.catalog);
        this.xstream.setReferenceByName(true);
        this.xp = this.xstream.getXStream();

        Assert.notNull(this.xp, "xStream persister should not be NULL");
        
        JobParameters jobParameters = this.currentJobExecution.getJobParameters();
        
        this.dryRun = Boolean.parseBoolean(jobParameters.getString(Backup.PARAM_DRY_RUN_MODE, "false"));
        this.bestEffort = Boolean.parseBoolean(jobParameters.getString(Backup.PARAM_BEST_EFFORT_MODE, "false"));
    }

    /**
     * @return the xp
     */
    public XStream getXp() {
        return xp;
    }

    /**
     * @param xp the xp to set
     */
    public void setXp(XStream xp) {
        this.xp = xp;
    }

    /**
     * @return the isNew
     */
    public boolean isNew() {
        return isNew;
    }

    /**
     * @return the currentJobExecution
     */
    public AbstractExecutionAdapter getCurrentJobExecution() {
        return currentJobExecution;
    }

    /**
     * @return the dryRun
     */
    public boolean isDryRun() {
        return dryRun;
    }

    /**
     * @return the bestEffort
     */
    public boolean isBestEffort() {
        return bestEffort;
    }
    
    @Override
    public T process(T resource) throws Exception {

        if (resource != null) {
            if (isNew) {
                // Disabling additional validators
                ((CatalogImpl) this.catalog).setExtendedValidation(false);
            }
            
            LOGGER.info("Processing resource: " + resource + 
                    " - Progress: [" + currentJobExecution.getExecutedSteps() + "/" + currentJobExecution.getTotalNumberOfSteps() + "]");

            if (resource instanceof WorkspaceInfo) {
                if (!validateWorkspace((WorkspaceInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    
                    logValidationExceptions(resource);
                    
                    return null;
                }
            } else if (resource instanceof DataStoreInfo) {
                if (!validateDataStore((DataStoreInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    
                    logValidationExceptions(resource);
                    
                    return null;
                }
                
                WorkspaceInfo ws = ((DataStoreInfo) resource).getWorkspace();
                if (this.catalog.getDefaultDataStore(ws) == null) {
                    this.catalog.setDefaultDataStore(ws, (DataStoreInfo) resource);
                }
                
            } else if (resource instanceof CoverageStoreInfo) {
                if (!validateCoverageStore((CoverageStoreInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    
                    logValidationExceptions(resource);
                    
                    return null;
                }
            }
            else if (resource instanceof ResourceInfo) {
                if (!validateResource((ResourceInfo) resource, isNew)) {
                    LOGGER.warning("Skipped invalid resource: " + resource);
                    
                    logValidationExceptions(resource);
                    
                    return null;
                }
            }
            else if (resource instanceof LayerInfo) {
                ValidationResult result = null;
                try {
                    result = this.catalog.validate((LayerInfo) resource, isNew);
                    if (!result.isValid()) {
                        
                        logValidationExceptions(resource);
                        
                        return null;
                    }
                } catch (Exception e) {
                    LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
                    
                    if(!bestEffort) {
                        if (result != null) {
                            result.throwIfInvalid();
                        } else {
                            throw e;
                        }
                    }

                    if(!bestEffort) {
                        getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    }
                    
                    return null;
                }
            }
            else if (resource instanceof StyleInfo) {
                ValidationResult result = null;
                try {
                    result = this.catalog.validate((StyleInfo) resource, isNew);
                    if (!result.isValid()) {
                        
                        logValidationExceptions(resource);
                        
                        return null;
                    }
                } catch (Exception e) {
                    if(!bestEffort) {
                        if (result != null) {
                            result.throwIfInvalid();
                        } else {
                            throw e;
                        }
                    }

                    if(!bestEffort) {
                        getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    }
                    
                    return null;
                }
            }
            else if (resource instanceof LayerGroupInfo) {
                ValidationResult result = null;
                try {
                    result = this.catalog.validate((LayerGroupInfo) resource, isNew);
                    if (!result.isValid()) {
                        
                        logValidationExceptions(resource);
                        
                        return null;
                    }
                } catch (Exception e) {
                    if(!bestEffort) {
                        if (result != null) {
                            result.throwIfInvalid();
                        } else {
                            throw e;
                        }
                    }

                    if(!bestEffort) {
                        getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    }
                    
                    return null;
                }
            }

            return resource;
        }

        return null;
    }

    /**
     * @param resource
     */
    private void logValidationExceptions(T resource) {
        CatalogException validationException = new CatalogException("Invalid resource: " + resource);
        if (!bestEffort) {
            getCurrentJobExecution().addFailureExceptions(Arrays.asList(validationException));
            throw validationException;
        } else {
            getCurrentJobExecution().addWarningExceptions(Arrays.asList(validationException));
        }
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
                result.throwIfInvalid();
                return false;
            }
        } catch (Exception e) {
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            
            if(!bestEffort) {
                getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                throw e;
            } else {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
            
            return false;
        }
        
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
                result.throwIfInvalid();
            }
        } catch (Exception e) {
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            
            if(!bestEffort) {
                getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                throw e;
            } else {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
            
            return false;
        }
        
        resource.setWorkspace(ws);

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
                result.throwIfInvalid();
            }
        } catch (Exception e) {
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            
            if(!bestEffort) {
                getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                throw e;
            } else {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
            
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
                CatalogException e = new CatalogException("Store is NULL for resource: " + resource);
                if(!bestEffort) {
                    getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    throw e;
                } else {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
                return false;
            }

            final Class storeClazz = (store instanceof DataStoreInfo ? DataStoreInfo.class : CoverageStoreInfo.class);
            final StoreInfo ds = this.catalog.getStoreByName(store.getName(), storeClazz);

            if (ds != null) {
                resource.setStore(ds);
            } else {
                CatalogException e = new CatalogException("Store is NULL for resource: " + resource);
                if(!bestEffort) {
                    getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    throw e;
                } else {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
                return false;
            }
            
            ResourceInfo existing = catalog.getResourceByStore( store, resource.getName(), ResourceInfo.class);
            if ( existing != null && !existing.getId().equals( resource.getId() ) ) {
                final String msg = "Resource named '"+resource.getName()+"' already exists in store: '"+ store.getName()+"'";
                CatalogException e = new CatalogException(msg);
                if(!bestEffort) {
                    getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    throw e;
                } else {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
                return false;
            }
            
            
            existing = catalog.getResourceByName( namespace, resource.getName(), ResourceInfo.class);
            if ( existing != null && !existing.getId().equals( resource.getId() ) ) {
                final String msg = "Resource named '"+resource.getName()+"' already exists in namespace: '"+ namespace.getPrefix()+"'";
                CatalogException e = new CatalogException(msg);
                if(!bestEffort) {
                    getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                    throw e;
                } else {
                    getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
                }
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOGGER.warning("Could not validate the resource " + resource + " due to the following issue: " + e.getLocalizedMessage());
            
            if(!bestEffort) {
                getCurrentJobExecution().addFailureExceptions(Arrays.asList(e));
                throw e;
            } else {
                getCurrentJobExecution().addWarningExceptions(Arrays.asList(e));
            }
            
            return false;
        }
    }
    
}
