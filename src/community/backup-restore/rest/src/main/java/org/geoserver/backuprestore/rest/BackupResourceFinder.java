/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import org.geoserver.rest.BeanResourceFinder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * REST resource finder for 
 * 
 * <pre>/br/backup[/&lt;backupId&gt;][.zip]</pre>
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class BackupResourceFinder extends BeanResourceFinder {

    /**
     * @param beanName
     */
    public BackupResourceFinder(String beanName) {
        super(beanName);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        BackupResource backupResource = (BackupResource) super.findTarget(request, response);
        
        if (request.getResourceRef().getLastSegment().endsWith(".zip")) {
            final BackupStreamResource backupStreamResource = new BackupStreamResource(backupResource.getBackupFacade());
            backupStreamResource.setRequest(request);
            backupStreamResource.setResponse(response);
            return backupStreamResource;
        } else {
            return backupResource;
        }
    }

    
}
