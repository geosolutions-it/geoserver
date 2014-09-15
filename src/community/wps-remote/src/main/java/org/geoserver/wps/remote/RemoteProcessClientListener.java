/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

import java.util.Map;

import org.geotools.data.Parameter;
import org.opengis.feature.type.Name;

/**
 * @author alessio.fabiani
 * 
 */
public abstract interface RemoteProcessClientListener {

    public void registerService(Name name, String title, String description,
            Map<String, Parameter<?>> paramInfo, Map<String, Parameter<?>> outputInfo,
            Map<String, Object> metadata);

    public void deregisterService(Name name);

    public void progress(final String pId, final Double progress);
    public void complete(final String pId, final Object outputs);
    public void exceptionOccurred(final String pId, Exception cause, Map<String, Object> metadata);
}
