/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.guid;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.geoserver.platform.exception.GeoServerException;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

/**
 * A simple per request cache making sure we don't hit the database more than once per second TODO:
 * make the timeout configurable
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class CachingGuidRuleDao implements GuidRuleDao {

    LoadingCache<String, List<GuidRule>> cache;

    private GuidRuleDao delegate;

    public CachingGuidRuleDao(final GuidRuleDao delegate) {
        this.delegate = delegate;
        cache = CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<GuidRule>>() {

                    @Override
                    public List<GuidRule> load(String guid) throws Exception {
                        return delegate.getRules(guid);
                    }
                });
    }

    @Override
    public List<GuidRule> getRules(String guid) throws GeoServerException {
        try {
            return cache.get(guid);
        } catch (ExecutionException e) {
            throw new GeoServerException(
                    "Failed to retrieve the list of rules from the cache/database", e);
        }
    }

    public void clearRules() throws GeoServerException {
        cache.invalidateAll();
        delegate.clearRules();
    }

    public void addRule(final GuidRule rule) throws GeoServerException {
        cache.invalidate(rule.getGuid());
        delegate.addRule(rule);
    }

}
