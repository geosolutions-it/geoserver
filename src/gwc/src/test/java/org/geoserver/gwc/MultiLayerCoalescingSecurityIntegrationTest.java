/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.LAKES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.filter.Filter;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.StorageObject;
import org.geowebcache.storage.TileObject;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Data-security coverage for multi-layer tile coalescing: a coalesced request where one member's effective read filter
 * is {@code Filter.EXCLUDE} for the requesting user must fall back to the live combined render rather than serve (or
 * populate) a cache entry from a per-user-filtered layer. Modeled on {@link GWCDataSecurityTest}'s
 * {@code TestResourceAccessManager} setup, which is the mechanism that exercises this specific case: the user is
 * authenticated and the layer is nameable, only its filtered content differs, so the request reaches
 * {@code WebMapService.getMap} instead of being rejected earlier by the security filter chain.
 */
public class MultiLayerCoalescingSecurityIntegrationTest extends GeoServerSystemTestSupport {

    private static final String RESTRICTED_USER = "restricted";

    private String layer1;

    private String layer2;

    private GridSubset gridSubset;

    /** Enable the Spring Security auth filters so requests carrying Basic auth are actually authenticated. */
    @Override
    protected List<jakarta.servlet.Filter> getFilters() {
        return Collections.singletonList((jakarta.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wms/ResourceAccessManagerContext.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setDirectWMSIntegrationEnabled(true);
        config.setMultiLayerCachingEnabled(true);
        config.setSecurityEnabled(true);
        gwc.saveConfig(config);

        GeoServerUserGroupStore userGroupStore = getSecurityManager()
                .loadUserGroupService(AbstractUserGroupService.DEFAULT_NAME)
                .createStore();
        userGroupStore.addUser(userGroupStore.createUserObject(RESTRICTED_USER, RESTRICTED_USER, true));
        userGroupStore.store();

        GeoServerRoleStore roleStore =
                getSecurityManager().getActiveRoleService().createStore();
        GeoServerRole role = roleStore.createRoleObject("ROLE_DUMMY");
        roleStore.addRole(role);
        roleStore.associateRoleToUser(role, RESTRICTED_USER);
        roleStore.store();

        Catalog catalog = getCatalog();
        FeatureTypeInfo restrictedLayer = catalog.getResourceByName(getLayerId(LAKES), FeatureTypeInfo.class);

        TestResourceAccessManager accessManager =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        // CatalogMode.HIDE + Filter.EXCLUDE makes the secured catalog return null from getLayerByName outright
        // (see GWC.verifyAccessLayer's own comment: "HIDE+EXCLUDE layers are already gone") - that fails even
        // earlier, at WMS KVP parsing, never reaching GWC at all. CHALLENGE keeps the layer nameable/resolvable
        // as a SecuredLayerInfo wrapping a Filter.EXCLUDE read filter, which is the case verifyAccessLayer checks.
        // Must be VectorAccessLimits (not the bare DataAccessLimits superclass): SecureFeatureSources, invoked while
        // GetMapKvpRequestReader validates the requested style against the feature type, rejects a plain
        // DataAccessLimits on a vector layer with "unexpected AccessLimits class".
        VectorAccessLimits excludeEverything =
                new VectorAccessLimits(CatalogMode.CHALLENGE, null, Filter.EXCLUDE, null, null);
        accessManager.putLimits(RESTRICTED_USER, restrictedLayer, excludeEverything);
    }

    @Before
    public void lookUpTestFixtures() throws Exception {
        layer1 = getLayerId(BASIC_POLYGONS);
        layer2 = getLayerId(LAKES);

        TileLayer tileLayer = GWC.get().getTileLayerByName(layer1);
        gridSubset = tileLayer.getGridSubset("EPSG:4326");
    }

    private String coalescedGetMap(String layers) {
        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        BoundingBox bounds = gridSubset.boundsFromIndex(tileIndex);

        return "wms?service=WMS&request=GetMap&version=1.1.1&format=image/png&transparent=true&tiled=true"
                + "&layers=" + layers
                + "&srs=" + gridSubset.getSRS()
                + "&width=" + gridSubset.getGridSet().getTileWidth()
                + "&height=" + gridSubset.getGridSet().getTileHeight()
                + "&bbox=" + bounds;
    }

    private TileObject sampleTile(String layerName) throws Exception {
        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        TileObject tileObject = TileObject.createQueryTileObject(
                layerName, tileIndex, gridSubset.getName(), "image/png", Collections.emptyMap());
        GWC.get().getCompositeBlobStore().get(tileObject);
        return tileObject;
    }

    @Test
    public void testCoalescedRequestFallsBackWhenAMembersReadFilterExcludesEverything() throws Exception {
        setRequestAuth(RESTRICTED_USER, RESTRICTED_USER);

        // GWC.verifyAccessLayer denies the coalesced shortcut for LAKES, falling back to a live combined render;
        // CatalogMode.CHALLENGE then denies THAT render too when it actually touches the restricted data. Both
        // paths agree: deny. That's the point of this test, not any specific GWC response header - CHALLENGE mode
        // throws before CachingWebMapService ever gets to set one.
        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

        assertNotEquals("image/png", response.getContentType());

        // the filtered member's cache was never populated by the coalesced attempt
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
    }

    @Test
    public void testCoalescedRequestSucceedsForAnUnrestrictedUser() throws Exception {
        setRequestAuth("admin", "geoserver");

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertNull(response.getHeader("geowebcache-miss-reason"));
    }
}
