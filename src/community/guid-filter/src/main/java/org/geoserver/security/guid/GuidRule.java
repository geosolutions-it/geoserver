/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import org.opengis.filter.Filter;

class GuidRule {
    String guid;

    String layerName;

    String userId;

    Filter filter;

    GuidRule() {
        super();
    }

    public GuidRule(String guid, String layerName, String userId, Filter filter) {
        super();
        this.guid = guid;
        this.layerName = layerName;
        this.userId = userId;
        this.filter = filter;
    }

    protected String getGuid() {
        return guid;
    }

    protected void setGuid(String guid) {
        this.guid = guid;
    }

    protected String getLayerName() {
        return layerName;
    }

    protected void setLayerName(String layerName) {
        this.layerName = layerName;
    }

    protected String getUserId() {
        return userId;
    }

    protected void setUserId(String userId) {
        this.userId = userId;
    }

    protected Filter getFilter() {
        return filter;
    }

    protected void setFilter(Filter filter) {
        this.filter = filter;
    }

}
