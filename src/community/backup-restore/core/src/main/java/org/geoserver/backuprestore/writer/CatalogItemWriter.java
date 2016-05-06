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
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 * 
 * Writes unmarshalled items into the temporary {@link Catalog} in memory.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogItemWriter<T> extends CatalogWriter<T> {

    public CatalogItemWriter(Class<T> clazz, Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(clazz, backupFacade, xStreamPersisterFactory);
    }

    @Override
    protected void beforeStep(StepExecution stepExecution) {
        if(this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
    }
    
    @Override
    public void write(List<? extends T> items) throws Exception {
        // TODO:
        
        for(T item : items) {
            // TODO: add items to the catalog
            //this.restoreCatalog.put(((Info)item).getId(), item);
            if(item instanceof WorkspaceInfo) {
                this.catalog.add((WorkspaceInfo)item);
            }
            else if(item instanceof NamespaceInfo) {
                this.catalog.add((NamespaceInfo)item);
            }
            else if(item instanceof DataStoreInfo) {
                this.catalog.add((DataStoreInfo)item);
            }
            else if(item instanceof CoverageStoreInfo) {
                this.catalog.add((CoverageStoreInfo)item);
            }
            else if(item instanceof ResourceInfo) {
                this.catalog.add((ResourceInfo)item);
            }
            else if(item instanceof LayerInfo) {
                this.catalog.add((LayerInfo)item);
            }
            else if(item instanceof StyleInfo) {
                this.catalog.add((StyleInfo)item);
            }
            else if(item instanceof LayerGroupInfo) {
                this.catalog.add((LayerGroupInfo)item);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void setResource(Resource resource) {
        // TODO Auto-generated method stub
        
    }

}
