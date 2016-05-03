/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.reader;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.util.CloseableIterator;
import org.opengis.filter.Filter;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.NonTransientResourceException;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;

/**
 * Concrete Spring Batch {@link ItemReader}.
 * 
 * Reads resource items from in memory {@link Catalog}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class CatalogItemReader<T> implements ItemReader<T> {

    Class clazz;
    Catalog catalog;
    CloseableIterator<T> catalogIterator;
    
    public CatalogItemReader(Class<T> clazz, Catalog catalog) {
        this.clazz = clazz;
        this.catalog = catalog;
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

}
