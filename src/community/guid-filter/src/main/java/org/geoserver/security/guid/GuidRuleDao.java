/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.util.List;

/**
 * A DAO that returns the rules for the specified guid
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface GuidRuleDao {

    public List<GuidRule> getRules(String guid);

    public abstract void clearRules();

    public abstract void addRule(final GuidRule rule);

}