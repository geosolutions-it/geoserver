/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.geoserver.data.test.MockData.BASIC_POLYGONS;
import static org.geoserver.data.test.MockData.FORESTS;
import static org.geoserver.data.test.MockData.LAKES;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geowebcache.grid.BoundingBox;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.storage.StorageException;
import org.geowebcache.storage.StorageObject;
import org.geowebcache.storage.TileObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** Multi-layer tile coalescing test. */
public class MultiLayerCoalescingIntegrationTest extends GeoServerSystemTestSupport {

    private String layer1;

    private String layer2;

    private GridSubset gridSubset;

    @Before
    public void enableMultiLayerCaching() throws Exception {
        layer1 = getLayerId(BASIC_POLYGONS);
        layer2 = getLayerId(LAKES);

        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setDirectWMSIntegrationEnabled(true);
        config.setMultiLayerCachingEnabled(true);
        gwc.saveConfig(config);

        TileLayer tileLayer = gwc.getTileLayerByName(layer1);
        gridSubset = tileLayer.getGridSubset("EPSG:4326");

        truncate(layer1);
        truncate(layer2);
    }

    @After
    public void resetConfig() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setDirectWMSIntegrationEnabled(false);
        config.setMultiLayerCachingEnabled(false);
        gwc.saveConfig(config);
    }

    private void truncate(String layerName) throws Exception {
        GWC.get().truncate(layerName);
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

    private TileObject sampleTile(String layerName) throws StorageException {
        long[] coverage = gridSubset.getCoverage(0);
        long[] tileIndex = {coverage[0], coverage[1], coverage[4]};
        TileObject tileObject = TileObject.createQueryTileObject(
                layerName, tileIndex, gridSubset.getName(), "image/png", Collections.emptyMap());
        GWC.get().getCompositeBlobStore().get(tileObject);
        return tileObject;
    }

    @Test
    public void testCoalescedRequest() throws Exception {
        // caches start empty
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());

        MockHttpServletResponse coalescedResponse = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, coalescedResponse.getStatus());
        assertEquals("image/png", coalescedResponse.getContentType());

        // stacking: the coalesced tile is neither member alone
        MockHttpServletResponse layer1Response = getAsServletResponse(coalescedGetMap(layer1));
        MockHttpServletResponse layer2Response = getAsServletResponse(coalescedGetMap(layer2));
        byte[] coalescedBytes = coalescedResponse.getContentAsByteArray();
        assertFalse(Arrays.equals(coalescedBytes, layer1Response.getContentAsByteArray()));
        assertFalse(Arrays.equals(coalescedBytes, layer2Response.getContentAsByteArray()));

        // cache population: each member's own tile cache now holds a real blob, keyed under its own layer name,
        // reusable by an ordinary single-layer request
        TileObject member1Tile = sampleTile(layer1);
        TileObject member2Tile = sampleTile(layer2);
        assertNotEquals(StorageObject.Status.MISS, member1Tile.getStatus());
        assertNotEquals(StorageObject.Status.MISS, member2Tile.getStatus());
        assertNotNull(member1Tile.getBlob());
        assertNotNull(member2Tile.getBlob());

        // a second coalesced request is now an all-cache-hit
        MockHttpServletResponse secondResponse = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, secondResponse.getStatus());
        assertEquals("HIT", secondResponse.getHeader("geowebcache-cache-result"));
        assertArrayEquals(coalescedBytes, secondResponse.getContentAsByteArray());
    }

    @Test
    public void testPartialCacheResultWhenOnlySomeMembersAreCached() throws Exception {
        // populate both members' caches
        getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertNotEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertNotEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());

        // evict just one member: the next coalesced request hits layer1 but re-renders layer2
        truncate(layer2);
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

        assertEquals(200, response.getStatus());
        assertEquals("PARTIAL 1/2", response.getHeader("geowebcache-cache-result"));

        // the re-rendered member is now cached again too
        assertNotEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
    }

    @Test
    public void testNonCacheableMemberIsLiveRenderedAndSplicedIntoTheStack() throws Exception {
        // populate layer1's own cache first
        getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertNotEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());

        GWC gwc = GWC.get();
        TileLayer forestsTileLayer = gwc.getTileLayerByName(getLayerId(FORESTS));
        forestsTileLayer.setEnabled(false);
        try {
            MockHttpServletResponse response =
                    getAsServletResponse(coalescedGetMap(layer1 + "," + getLayerId(FORESTS)));

            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            // layer1 is a cache hit, the disabled member always misses
            assertEquals("PARTIAL 1/2", response.getHeader("geowebcache-cache-result"));

            // stacking happened: the coalesced result differs from layer1 rendered alone
            byte[] coalescedBytes = response.getContentAsByteArray();
            MockHttpServletResponse layer1Response = getAsServletResponse(coalescedGetMap(layer1));
            assertFalse(Arrays.equals(coalescedBytes, layer1Response.getContentAsByteArray()));
        } finally {
            forestsTileLayer.setEnabled(true);
        }
    }

    @Test
    public void testPartialCacheResultCountsEveryMemberOfABatchedLiveRun() throws Exception {
        // populate both cacheable members' caches first
        getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertNotEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertNotEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());

        GWC gwc = GWC.get();
        TileLayer forestsTileLayer = gwc.getTileLayerByName(getLayerId(FORESTS));
        forestsTileLayer.setEnabled(false);
        try {
            String forests = getLayerId(FORESTS);
            String layers = layer1 + "," + forests + "," + forests + "," + layer2;
            MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layers));

            assertEquals(200, response.getStatus());
            // 4 original members: layer1 and layer2 are cache hits, both forests slots always miss
            assertEquals("PARTIAL 2/4", response.getHeader("geowebcache-cache-result"));
        } finally {
            forestsTileLayer.setEnabled(true);
        }
    }

    @Test
    public void testCacheControl() throws Exception {
        setCachingMetadata(layer1, true, 600);
        setCachingMetadata(layer2, true, 300);

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, response.getStatus());
        assertEquals("max-age=300, must-revalidate", response.getHeader("Cache-Control"));
        assertNotNull(response.getHeader("Expires"));
    }

    @Test
    public void testNoCacheControlWhenAnyLayerDisablesCaching() throws Exception {
        setCachingMetadata(layer1, true, 600);
        setCachingMetadata(layer2, false, 0);

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Cache-Control"));
    }

    @Test
    public void testLiveRender() throws Exception {
        GWC gwc = GWC.get();
        GWCConfig config = gwc.getConfig();
        config.setMultiLayerCachingEnabled(false);
        gwc.saveConfig(config);

        MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));
        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
        assertTrue(response.getHeader("geowebcache-miss-reason").contains("more than one layer requested"));

        // and neither member's cache was touched
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
    }

    @Test
    public void testWithLabeledStyle() throws Exception {
        getTestData().addStyle("labeled", "labeled.sld", MultiLayerCoalescingIntegrationTest.class, getCatalog());

        // layer1 keeps its default style, layer2 uses the labeled one (positional, aligned to LAYERS)
        String url = coalescedGetMap(layer1 + "," + layer2) + "&styles=,labeled";
        MockHttpServletResponse response = getAsServletResponse(url);

        assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
        assertTrue(response.getHeader("geowebcache-miss-reason").contains("draws labels or composites"));

        // the rejected coalesced attempt never populated either member's cache
        assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
        assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
    }

    @Test
    public void testWithCqlFilters() throws Exception {
        GeoServerTileLayer tileLayer1 = (GeoServerTileLayer) GWC.get().getTileLayerByName(layer1);
        GeoServerTileLayer tileLayer2 = (GeoServerTileLayer) GWC.get().getTileLayerByName(layer2);
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayer1.getInfo(), "CQL_FILTER", true);
        TileLayerInfoUtil.updateAcceptAllRegExParameterFilter(tileLayer2.getInfo(), "CQL_FILTER", true);
        GWC.get().save(tileLayer1);
        GWC.get().save(tileLayer2);

        // one clause per member, aligned to LAYERS order; each member must see only its own slice
        String url = coalescedGetMap(layer1 + "," + layer2) + "&CQL_FILTER=INCLUDE;EXCLUDE";
        MockHttpServletResponse response = getAsServletResponse(url);

        assertEquals(200, response.getStatus());
        assertEquals("image/png", response.getContentType());
        // reaching a real cache dispatch
        assertNull(response.getHeader("geowebcache-miss-reason"));
        assertNotNull(response.getHeader("geowebcache-cache-result"));
    }

    @Test
    public void testMemoryLimits() throws Exception {
        // GWC's projected peak is (members + 1) * tileWidth * tileHeight * 4 bytes ARGB = 3 * 256 * 256 * 4 = 768 KB
        // for this 2-member request. The live fallback render's own memory check (RenderedImageMapOutputFormat)
        // uses a different, much smaller estimate: just the output buffer (256 * 256 * 4 = 256 KB) plus per-style
        // back-buffers, which is 0 here since both layers use plain single-pass styles. 400 KB sits between the
        // two: below GWC's conservative peak (guard fires) but above what the live render actually needs (fallback
        // succeeds instead of also being denied by the live path's own limit).
        setMaxRequestMemory(400);
        try {
            MockHttpServletResponse response = getAsServletResponse(coalescedGetMap(layer1 + "," + layer2));

            // the guard is a fallback, not a hard error: the live combined render still succeeds
            assertEquals(200, response.getStatus());
            assertEquals("image/png", response.getContentType());
            assertEquals("MISS", response.getHeader("geowebcache-cache-result"));
            assertTrue(response.getHeader("geowebcache-miss-reason").contains("exceed max request memory"));

            // fired before loading any tile: neither member's cache was touched
            assertEquals(StorageObject.Status.MISS, sampleTile(layer1).getStatus());
            assertEquals(StorageObject.Status.MISS, sampleTile(layer2).getStatus());
        } finally {
            setMaxRequestMemory(0);
        }
    }

    private void setMaxRequestMemory(int kilobytes) throws Exception {
        GeoServer geoServer = getGeoServer();
        WMSInfo wms = geoServer.getService(WMSInfo.class);
        wms.setMaxRequestMemory(kilobytes);
        geoServer.save(wms);
    }

    private void setCachingMetadata(String layerId, boolean cachingEnabled, int cacheAgeMax) throws Exception {
        FeatureTypeInfo ft = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
        ft.getMetadata().put(ResourceInfo.CACHING_ENABLED, cachingEnabled);
        ft.getMetadata().put(ResourceInfo.CACHE_AGE_MAX, cacheAgeMax);
        getCatalog().save(ft);
    }
}
