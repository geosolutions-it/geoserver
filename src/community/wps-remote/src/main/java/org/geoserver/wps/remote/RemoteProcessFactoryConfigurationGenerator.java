/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

/**
 * Simple interface to get the {@link RemoteProcessFactoryConfiguration}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public interface RemoteProcessFactoryConfigurationGenerator {

    /**
     * @return the {@link RemoteProcessFactoryConfiguration} object
     */
    public RemoteProcessFactoryConfiguration getConfiguration();

}
