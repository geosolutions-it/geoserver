/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.reader;

import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.opengis.filter.Filter;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.core.io.Resource;

/**
 * Concrete Spring Batch {@link ItemReader}.
 * 
 * Reads resource items from in memory {@link Catalog}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogItemReader<T> extends CatalogReader<T> {

    CloseableIterator<T> catalogIterator;
    
    public CatalogItemReader(Class<T> clazz, Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(clazz, backupFacade, xStreamPersisterFactory);
    }
    
    protected void beforeStep(StepExecution stepExecution) {
        this.catalogIterator = (CloseableIterator<T>) catalog.list(this.clazz, Filter.INCLUDE);
    }
    
    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException,
            NonTransientResourceException {
        if (catalogIterator.hasNext()) {
            return (T) catalogIterator.next();
        }
        
        return null;
    }

    @Override
    public void setResource(Resource resource) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected T doRead() throws Exception {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void doOpen() throws Exception {
        // TODO Auto-generated method stub
        
    }

    @Override
    protected void doClose() throws Exception {
        // TODO Auto-generated method stub
        
    }

}
