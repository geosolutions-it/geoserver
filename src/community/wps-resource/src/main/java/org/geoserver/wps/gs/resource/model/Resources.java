/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model;

import java.util.ArrayList;
import java.util.List;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;

/**
 * A bean collecing a list of {@link Resource}s. Useful for XStream Alias mapping purposes.
 * 
 * @author alessio.fabiani
 * 
 */
@XStreamAlias("resources")
public class Resources {

    @XStreamImplicit
    private List<Resource> resources = new ArrayList<Resource>();

    /**
     * @return the resources
     */
    public List<Resource> getResources() {
        return resources;
    }

    /**
     * @param resources the resources to set
     */
    public void setResources(List<Resource> resources) {
        this.resources = resources;
    }

}
