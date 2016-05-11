/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.io.IOException;

import org.geoserver.backuprestore.Backup;
import org.geoserver.platform.resource.Resource;

/**
 * TODO
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public interface CatalogAdditionalResourcesWriter<T> {

    public boolean canHandle(Object item);
    
    public void writeAdditionalResources(Backup backupFacade, Resource base, T item) throws IOException;
    
}
