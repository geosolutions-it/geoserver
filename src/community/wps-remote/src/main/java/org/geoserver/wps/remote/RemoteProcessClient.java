/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;

import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * @author alessio.fabiani
 *
 */
public interface RemoteProcessClient {

    public void init() throws Exception;
    
    public void destroy() throws Exception;
    
    public boolean isEnabled();
    
    public void registerListener(RemoteProcessClientListener listener);
    
    public void deregisterListener(RemoteProcessClientListener listener);

    public String execute(Name name, Map<String, Object> input, Map<String, Object> metadata, ProgressListener monitor) throws Exception;
    
}
