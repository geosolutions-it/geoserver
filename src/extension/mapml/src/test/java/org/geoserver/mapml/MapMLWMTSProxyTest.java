/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geowebcache.grid.GridSubsetFactory.createGridSubSet;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.mapml.gwc.gridset.MapMLGridsets;
import org.geowebcache.grid.GridSubset;
import org.geowebcache.mime.TextMime;
import org.junit.BeforeClass;
import org.junit.Test;

public class MapMLWMTSProxyTest extends MapMLBaseProxyTest {

    protected static final String BASE_WMTS_REQUEST =
            "wms?LAYERS=cascadedLayer"
                    + "&STYLES=&FORMAT="
                    + MapMLConstants.MAPML_MIME_TYPE
                    + "&SERVICE=WMS&VERSION=1.1.0"
                    + "&REQUEST=GetMap"
                    + "&SRS=MapML:OSMTILE"
                    + "&BBOX=-1.3885038382960921E7,2870337.130793682,-7455049.489182421,6338174.0557576185"
                    + "&WIDTH=768"
                    + "&HEIGHT=414"
                    + "&format_options="
                    + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                    + ":image/png";

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        super.registerNamespaces(namespaces);
        namespaces.put("wmts", "http://www.opengis.net/wmts/1.0");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink=", "http://www.w3.org/1999/xlink");
    }

    @BeforeClass
    public static void beforeClass() {
        initMockService(
                "/mockgeoserver",
                "/gwc/service/wmts",
                "REQUEST=GetCapabilities&VERSION=1.0.0&SERVICE=WMTS",
                "wmtscaps.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        WMTSStoreInfo wmtsStore = catalog.getFactory().createWebMapTileServer();
        wmtsStore.setName("wmtsStore");
        wmtsStore.setWorkspace(catalog.getDefaultWorkspace());
        wmtsStore.setCapabilitiesURL(
                "http://localhost:" + mockService.port() + getCapabilitiesURL());
        wmtsStore.setEnabled(true);
        catalog.add(wmtsStore);

        // Create WMSLayerInfo using the Catalog factory
        WMTSLayerInfo wmtsLayer = catalog.getFactory().createWMTSLayer();
        wmtsLayer.setName("cascadedLayer");
        wmtsLayer.setNativeName("topp:states");
        wmtsLayer.setStore(wmtsStore);
        wmtsLayer.setAdvertised(true);
        wmtsLayer.setEnabled(true);

        // Add the layer to the catalog
        LayerInfo layerInfo = catalog.getFactory().createLayer();
        layerInfo.setResource(wmtsLayer);
        layerInfo.setDefaultStyle(catalog.getStyleByName("default"));
        catalog.add(wmtsLayer);
        catalog.add(layerInfo);

        GWC gwc = applicationContext.getBean(GWC.class);
        GWCConfig defaults = GWCConfig.getOldDefaults();
        // it seems just the fact of retrieving the bean causes the
        // GridSets to be added to the gwc GridSetBroker, but if you don't do
        // this, they are not added automatically
        MapMLGridsets mgs = applicationContext.getBean(MapMLGridsets.class);
        GridSubset wgs84gridset = createGridSubSet(mgs.getGridSet("WGS84").get());
        GridSubset osmtilegridset = createGridSubSet(mgs.getGridSet("OSMTILE").get());

        GeoServerTileLayer layerInfoTileLayer =
                new GeoServerTileLayer(layerInfo, defaults, gwc.getGridSetBroker());
        layerInfoTileLayer.addGridSubset(wgs84gridset);
        layerInfoTileLayer.addGridSubset(osmtilegridset);
        layerInfoTileLayer.getInfo().getMimeFormats().add(TextMime.txtMapml.getMimeType());
        gwc.save(layerInfoTileLayer);
    }

    @Test
    public void testRemoteVsNotRemote() throws Exception {
        Catalog cat = getCatalog();
        // Verify the layer was added
        LayerInfo li = cat.getLayerByName("cascadedLayer");
        assertNotNull(li);
        assertEquals("cascadedLayer", li.getName());

        ResourceInfo layerMeta = li.getResource();
        layerMeta.getMetadata().put("mapml.useRemote", false);
        layerMeta.getMetadata().put("mapml.useTiles", true);
        cat.save(layerMeta);

        // get the mapml doc for the layer
        String path = BASE_WMTS_REQUEST;

        checkCascading(path, false, "tile");

        // Now switching to use Remote URL
        layerMeta.getMetadata().put("mapml.useRemote", true);
        cat.save(layerMeta);

        checkCascading(path, true, "tile");
    }

    @Override
    protected void assertCascading(boolean shouldCascade, String url) {
        assertTrue(url.contains("service=WMTS"));
        if (shouldCascade) {
            // The remote capabilities defines a custom GridSet that matches
            // the OSMTILE with a different name: MATCHING_OSMTILE
            // Identifiers are also not simple numbers but contain a common prefix.
            assertTrue(
                    url.startsWith(
                            "http://localhost:" + mockService.port() + MOCK_SERVER + CONTEXT));
            assertTrue(url.contains("layer=topp:states"));
            assertTrue(url.contains("tilematrixset=MATCHING_OSMTILE"));
            // Common prefix has been pre-pended to the tilematrix z input
            assertTrue(url.contains("tilematrix=OSM:{z}"));
            // GetFeatureInfo
            if (url.contains("infoformat")) {
                assertTrue(url.contains("infoformat=text/html"));
            }
        } else {
            assertTrue(url.startsWith("http://localhost:8080/geoserver" + CONTEXT));
            assertTrue(url.contains("layer=gs:cascadedLayer"));
            assertTrue(url.contains("tilematrixset=OSMTILE"));
            assertTrue(url.contains("tilematrix={z}"));
            // GetFeatureInfo
            if (url.contains("infoformat")) {
                assertTrue(url.contains("infoformat=text/mapml"));
            }
        }
    }
}
