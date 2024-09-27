/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

public class MapMLWMSGetFeatureInfoProxyTest extends MapMLBaseProxyTest {

    @BeforeClass
    public static void beforeClass() {
        initMockService(
                "/mockgeoserver",
                "/wms",
                "REQUEST=GetCapabilities&VERSION=1.3.0&SERVICE=WMS",
                "wmscapsgmlfeatureinfo.xml");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();

        WMSStoreInfo wmsStore = catalog.getFactory().createWebMapServer();
        wmsStore.setName("wmsStore");
        wmsStore.setWorkspace(catalog.getDefaultWorkspace());
        wmsStore.setCapabilitiesURL(
                "http://localhost:" + mockService.port() + getCapabilitiesURL());
        wmsStore.setEnabled(true);
        catalog.add(wmsStore);

        // Create WMSLayerInfo using the Catalog factory
        WMSLayerInfo wmsLayer = catalog.getFactory().createWMSLayer();
        wmsLayer.setName("cascadedLayer");
        wmsLayer.setNativeName("topp:states");
        wmsLayer.setStore(wmsStore);
        wmsLayer.setAdvertised(true);
        wmsLayer.setEnabled(true);

        // Add the layer to the catalog
        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(wmsLayer);
        layer.setDefaultStyle(catalog.getStyleByName("default"));
        catalog.add(wmsLayer);
        catalog.add(layer);
    }

    @Test
    public void testMapMLLocalQueryLink() throws Exception {
        Catalog cat = getCatalog();
        // Verify the layer was added
        LayerInfo layerInfo = cat.getLayerByName("cascadedLayer");
        ResourceInfo layerMeta = layerInfo.getResource();
        layerMeta.getMetadata().put("mapml.useRemote", false);
        cat.save(layerMeta);
        String path = BASE_REQUEST;
        Document doc = getMapML(path);

        String url = xpath.evaluate("//html:map-link[@rel='query']/@tref", doc);
        assertTrue(url.startsWith("http://localhost:8080/geoserver" + CONTEXT));
        assertTrue(url.contains("layers=cascadedLayer"));
        assertTrue(url.contains("request=GetFeatureInfo"));

        layerMeta.getMetadata().put("mapml.useRemote", true);
        cat.save(layerMeta);

        // The sample getCapabilities is only returning GML as supported
        // Formats for the GetFeatureInfo so the query link will not be generated
        // when cascading
        doc = getMapML(path);
        url = xpath.evaluate("//html:map-link[@rel='query']/@tref", doc);
        assertTrue(url.isEmpty());
    }
}
