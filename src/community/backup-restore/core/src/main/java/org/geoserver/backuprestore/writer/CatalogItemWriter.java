/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.util.List;

import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.InitializingBean;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 * 
 * Writes unmarshalled items into the temporary {@link Catalog} in memory.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogItemWriter<CatalogInfo> implements ItemWriter<CatalogInfo>, InitializingBean {

    Class clazz;
    
    Backup backupFacade;

    private Catalog restoreCatalog;
    
    public CatalogItemWriter(Class<CatalogInfo> clazz, Backup backupFacade) {
        this.clazz = clazz;
        this.backupFacade = backupFacade;
    }

    protected String getItemName(XStreamPersister xp) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

    @BeforeStep
    protected void retrieveInterstepData(StepExecution stepExecution) {
        JobExecution jobExecution = stepExecution.getJobExecution();
        ExecutionContext jobContext = jobExecution.getExecutionContext();
        this.restoreCatalog = backupFacade.getRestoreExecutions().get(jobExecution.getId()).getRestoreCatalog();
    }
    
    @Override
    public void write(List<? extends CatalogInfo> items) throws Exception {
        // TODO:
        
        for(CatalogInfo item : items) {
            // TODO: add items to the restoreCatalog
            //this.restoreCatalog.put(((Info)item).getId(), item);
            if(item instanceof WorkspaceInfo) {
                this.restoreCatalog.add((WorkspaceInfo)item);
            }
            else if(item instanceof NamespaceInfo) {
                this.restoreCatalog.add((NamespaceInfo)item);
            }
            else if(item instanceof DataStoreInfo) {
                this.restoreCatalog.add((DataStoreInfo)item);
            }
            else if(item instanceof CoverageStoreInfo) {
                this.restoreCatalog.add((CoverageStoreInfo)item);
            }
            else if(item instanceof ResourceInfo) {
                this.restoreCatalog.add((ResourceInfo)item);
            }
            else if(item instanceof LayerInfo) {
                this.restoreCatalog.add((LayerInfo)item);
            }
            else if(item instanceof StyleInfo) {
                this.restoreCatalog.add((StyleInfo)item);
            }
            else if(item instanceof LayerGroupInfo) {
                this.restoreCatalog.add((LayerGroupInfo)item);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub
        
    }

}
