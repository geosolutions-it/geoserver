/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.MultiPolygon;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Test;

public class MapMLWMSFeatureTest extends MapMLTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("polygonFilter", "polygonFilter.sld", getClass(), catalog);
        testData.addStyle("polygonElseFilter", "polygonElseFilter.sld", getClass(), catalog);
        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);
    }

    @After
    public void tearDown() {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(lgi);

        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(liRaster);
    }

    @Test
    public void testMapMLUseFeatures() throws Exception {

        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        Mapml mapmlFeatures =
                getWMSAsMapML(
                        MockData.BASIC_POLYGONS.getLocalPart(),
                        null,
                        null,
                        "-180,-90,180,90",
                        "EPSG:4326",
                        null,
                        true);

        assertEquals(
                "Basic Polygons layer has three features, so one should show up in the conversion",
                3,
                mapmlFeatures.getBody().getFeatures().size());
        assertEquals(
                "Polygons layer coordinates should match original feature's coordinates",
                "-1,0,0,1,1,0,0,-1,-1,0",
                ((MultiPolygon)
                                mapmlFeatures
                                        .getBody()
                                        .getFeatures()
                                        .get(0)
                                        .getGeometry()
                                        .getGeometryContent()
                                        .getValue())
                        .getPolygon().get(0).getThreeOrMoreCoordinatePairs().get(0).getValue()
                                .stream()
                                .collect(Collectors.joining(",")));
    }

    @Test
    public void testMapMLUseFeaturesWithSLDFilter() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("polygonFilter"));
        li.getStyles().add(cat.getStyleByName("polygonElseFilter"));
        li.setDefaultStyle(cat.getStyleByName("polygonFilter"));
        cat.save(li);
        Mapml mapmlFeatures =
                getWMSAsMapML(
                        MockData.BUILDINGS.getLocalPart(),
                        null,
                        null,
                        null,
                        "EPSG:4326",
                        "polygonFilter",
                        true);

        assertEquals(
                "Buildings layer has two features, only one should show up after the SLD is applied",
                1,
                mapmlFeatures.getBody().getFeatures().size());

        Mapml mapmlFeaturesElse =
                getWMSAsMapML(
                        MockData.BUILDINGS.getLocalPart(),
                        null,
                        null,
                        null,
                        "EPSG:4326",
                        "polygonElseFilter",
                        true);

        assertEquals(
                "Buildings layer has two features, both should show up after the SLD with elseFilter is applied",
                2,
                mapmlFeaturesElse.getBody().getFeatures().size());
    }

    @Test
    public void testExceptionBecauseMoreThanOneFeatureType() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        li.getMetadata().put(MAPML_USE_FEATURES, true);
        li.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);
        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, true);
        lgi.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(lgi);
        String response =
                getWMSAsMapMLString(
                        "layerGroup" + "," + MockData.BASIC_POLYGONS.getLocalPart(),
                        null,
                        null,
                        null,
                        "EPSG:4326",
                        null,
                        true);

        assertTrue(
                "MapML response contains an exception due to multiple feature types",
                response.contains(
                        "MapML WMS Feature format does not currently support Multiple Feature Type output."));
    }

    @Test
    public void testExceptionBecauseBecauseRaster() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getMetadata().put(MAPML_USE_FEATURES, true);
        liRaster.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(liRaster);
        String response =
                getWMSAsMapMLString(
                        MockData.WORLD.getLocalPart(), null, null, null, "EPSG:3857", null, true);

        assertTrue(
                "MapML response contains an exception due to non-vector type",
                response.contains(
                        "MapML WMS Feature format does not currently support non-vector layers."));
    }
}
