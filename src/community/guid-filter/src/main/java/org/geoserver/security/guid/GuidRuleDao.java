/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.sql.Types;
import java.util.List;

import org.geoserver.platform.exception.GeoServerException;
import org.geoserver.security.guid.GuidTransactionTemplate.GuidTransactionCallback;
import org.geotools.filter.text.ecql.ECQL;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.TransactionException;
import org.springframework.transaction.TransactionStatus;

/**
 * A DAO that returns the rules for the specified guid
 * 
 * @author Andrea Aime - GeoSolutions
 */
public interface GuidRuleDao {

    public List<GuidRule> getRules(String guid) throws GeoServerException;

    public abstract void clearRules() throws TransactionException, GeoServerException;

    public abstract void addRule(final GuidRule rule) throws GeoServerException;

}