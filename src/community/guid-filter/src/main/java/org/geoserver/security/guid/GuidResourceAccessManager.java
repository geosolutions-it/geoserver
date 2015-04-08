/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.LayerGroupAccessLimits;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.StyleAccessLimits;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.springframework.security.core.Authentication;

/**
 * A resource access manager limiting layers based on a GUID parameter in the request URL, and a
 * lookup table with rules on which layers/filters to apply when reading
 * 
 * @author Andrea Aime - GeoSolutions
 *
 */
public class GuidResourceAccessManager extends AbstractDispatcherCallback implements
        ResourceAccessManager {

    static final Logger LOGGER = Logging.getLogger(GuidResourceAccessManager.class);

    static final CatalogMode CATALOG_MODE = CatalogMode.HIDE;

    static final List<GuidRule> NO_LIMITS = null;

    static final String GUID = "guid";

    static final WorkspaceAccessLimits WS_ACCESS_LIMITS = new WorkspaceAccessLimits(CATALOG_MODE,
            true, true);

    GuidRuleDao rulesDao;

    public GuidResourceAccessManager(GuidRuleDao rules) {
        this.rulesDao = rules;
    }

    @Override
    public WorkspaceAccessLimits getAccessLimits(Authentication user, WorkspaceInfo workspace) {
        // we do limit access on a layer basis, not on workspace
        return WS_ACCESS_LIMITS;
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, LayerInfo layer) {
        return getAccessLimits(user, layer.getResource());
    }

    @Override
    public DataAccessLimits getAccessLimits(Authentication user, ResourceInfo resource) {
        List<GuidRule> rules = getGuidRules();
        if (rules == NO_LIMITS) {
            return null;
        }

        // Locate the rule that works on this layer
        String prefixedName = resource.prefixedName();
        for (GuidRule rule : rules) {
            if (prefixedName.equals(rule.getLayerName())) {
                return new VectorAccessLimits(CATALOG_MODE, null, rule.getFilter(), null,
                        rule.getFilter());
            }
        }

        // if we got here, we did not find any matching rule
        return new DataAccessLimits(CATALOG_MODE, Filter.EXCLUDE);
    }

    @Override
    public StyleAccessLimits getAccessLimits(Authentication user, StyleInfo style) {
        // no limits
        return null;
    }

    @Override
    public LayerGroupAccessLimits getAccessLimits(Authentication user, LayerGroupInfo layerGroup) {
        // no limits
        return null;
    }

    @Override
    public Filter getSecurityFilter(Authentication user, Class<? extends CatalogInfo> clazz) {
        List<GuidRule> rules = getGuidRules();
        if (rules == NO_LIMITS) {
            return Filter.INCLUDE;
        }

        if (PublishedInfo.class.isAssignableFrom(clazz)
                || ResourceInfo.class.isAssignableFrom(clazz)) {
            List<Filter> filters = new ArrayList<>();
            for (GuidRule rule : rules) {
                String layerName = rule.getLayerName();
                Filter nameFilter = Predicates.equal("prefixedName", layerName);
                filters.add(nameFilter);
            }
            if (filters.size() > 1) {
                return Predicates.or(filters);
            } else {
                return filters.get(0);
            }
        } else {
            return Filter.INCLUDE;
        }

    }

    private List<GuidRule> getGuidRules() {
        Request request = Dispatcher.REQUEST.get();
        if (request == null) {
            LOGGER.log(Level.FINE, "Could not find a request, thus assuming no limits");
            return NO_LIMITS;
        }

        String guid = KvpUtils.getSingleValue(request.getRawKvp(), GUID);
        if (guid == null) {
            LOGGER.log(Level.FINE,
                    "Could not find a GUID in the request, applying no security limist");
            return NO_LIMITS;
        }

        List<GuidRule> rules = rulesDao.getRules(guid);
        // if the guid is specified but not knows, by spec we throw an exception
        if (rules == null || rules.isEmpty()) {
            // we have the REQUEST of the OWS dispatcher, so it's a OGC request
            throw new ServiceException("Invalid guid value '" + guid + "'",
                    ServiceException.INVALID_PARAMETER_VALUE, "guid");
        }
        return rules;
    }

    @Override
    public org.geoserver.platform.Operation operationDispatched(Request request,
            org.geoserver.platform.Operation operation) {
        // used to check if the guid is valid before running the request
        getGuidRules();

        return operation;
    }

}
