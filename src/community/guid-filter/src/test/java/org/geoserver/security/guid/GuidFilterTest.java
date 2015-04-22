package org.geoserver.security.guid;

import static org.junit.Assert.assertEquals;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wms.WMSInfo;
import org.geotools.filter.text.ecql.ECQL;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.w3c.dom.Document;

public class GuidFilterTest extends GeoServerSystemTestSupport {

    private static final String GUID_ABC = "abc";

    private static final String GROUP = "group";

    private GuidRule abcAllPolygons;

    private GuidRuleDao dao;

    private XpathEngine xpath;

    private GuidRule abcAllLakes;

    private GuidRule abcBuildingsMainStreet;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // prepare the connection properties for an embedded database
        Properties props = new Properties();
        props.put("driver", "org.h2.Driver");
        props.put("url", "jdbc:h2:target/geotools");
        props.put("user", "geotools");
        props.put("password", "geotools");
        props.put("validationQuery", "select 1");

        // write out the config in a data directory
        GeoServerDataDirectory dd = getDataDirectory();
        Resource propertiesFile = dd.getRoot(ConfigurableDataSource.GUID_PROPERTIES);
        try (OutputStream os = propertiesFile.out()) {
            props.store(os, "Test connection properties");
        }

        // register wms and wfs namespaces and setup xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");

        testData.registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // reduce the caps document size
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        wms.getSRS().add("EPSG:4326");
        getGeoServer().save(wms);

        // create a group, so that we check filtering them out in caps documents
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        if (lakes != null && forests != null) {
            group.setName(GROUP);
            group.getLayers().add(lakes);
            group.getLayers().add(forests);
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.calculateLayerGroupBounds(group);
            catalog.add(group);
        }
    }

    @Before
    public void prepare() throws Exception {
        // the dao
        dao = applicationContext.getBean(CachingGuidRuleDao.class);

        // the rules used for testing
        abcAllPolygons = new GuidRule(GUID_ABC, getLayerId(MockData.POLYGONS), "user00",
                Filter.INCLUDE);
        abcAllLakes = new GuidRule(GUID_ABC, getLayerId(MockData.LAKES), "user00", Filter.INCLUDE);
        abcBuildingsMainStreet = new GuidRule(GUID_ABC, getLayerId(MockData.BUILDINGS), "user00",
                ECQL.toFilter("ADDRESS = '123 Main Street'"));

        // make sure we start empty
        dao.clearRules(null);

        // xpath for assertions
        xpath = XMLUnit.newXpathEngine();
    }

    @Test
    public void testWmsNoGuid() throws Exception {
        dao.addRule(abcAllPolygons);

        // we need to only count geometryless layers
        int layersCount = 0;
        for (LayerInfo li : getCatalog().getLayers()) {
            if (li.getResource() instanceof FeatureTypeInfo) {
                FeatureTypeInfo fti = (FeatureTypeInfo) li.getResource();
                if (fti.getFeatureType().getGeometryDescriptor() != null) {
                    layersCount++;
                }
            } else {
                layersCount++;
            }
        }

        int groupCount = getCatalog().getLayerGroups().size();

        Document dom = getAsDOM("wms?request=getCapabilities&version=1.1.1");
        assertEquals(layersCount + groupCount,
                xpath.getMatchingNodes("//Capability/Layer/Layer", dom).getLength());
    }

    @Test
    public void testWfsNoGuid() throws Exception {
        dao.addRule(abcAllPolygons);

        // we need to only count geometryless layers
        int featureTypeCount = getCatalog().getFeatureTypes().size();

        Document dom = getAsDOM("wfs?request=getCapabilities&version=1.1.0");
        // print(dom);
        assertEquals(featureTypeCount, xpath.getMatchingNodes("//wfs:FeatureType", dom).getLength());
    }

    @Test
    public void testInvalidGuid() throws Exception {
        dao.addRule(abcAllPolygons);

        Document doc = getAsDOM("wms?request=getCapabilities&version=1.3.0&guid=notThere");
        // print(doc);
        XMLAssert.assertXpathEvaluatesTo("1",
                "count(//ogc:ServiceExceptionReport/ogc:ServiceException)", doc);
        XMLAssert.assertXpathEvaluatesTo(ServiceException.INVALID_PARAMETER_VALUE,
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@code", doc);
        XMLAssert.assertXpathEvaluatesTo("guid",
                "/ogc:ServiceExceptionReport/ogc:ServiceException/@locator", doc);
    }

    @Test
    public void testWmsCapabilitiesSingleRule() throws Exception {
        dao.addRule(abcAllPolygons);

        Document dom = getAsDOM("wms?request=getCapabilities&version=1.1.0&guid=" + GUID_ABC);
        // print(dom);
        assertEquals(1, xpath.getMatchingNodes("//Capability/Layer/Layer", dom).getLength());
        assertEquals(getLayerId(MockData.POLYGONS),
                xpath.evaluate(("//Capability/Layer/Layer/Name"), dom));

    }

    @Test
    public void testWmsCapabilitiesTwoRules() throws Exception {
        dao.addRule(abcAllPolygons);
        dao.addRule(abcAllLakes);

        Document dom = getAsDOM("wms?request=getCapabilities&version=1.1.0&guid=" + GUID_ABC);
        // print(dom);
        assertEquals(2, xpath.getMatchingNodes("//Capability/Layer/Layer", dom).getLength());
        assertEquals(
                1,
                xpath.getMatchingNodes(
                        ("//Capability/Layer/Layer[Name = '" + getLayerId(MockData.POLYGONS) + "']"),
                        dom).getLength());
        assertEquals(
                1,
                xpath.getMatchingNodes(
                        ("//Capability/Layer/Layer[Name = '" + getLayerId(MockData.LAKES) + "']"),
                        dom).getLength());
    }

    @Test
    public void testWfsCapabilitiesTwoRules() throws Exception {
        dao.addRule(abcAllPolygons);
        dao.addRule(abcAllLakes);

        Document dom = getAsDOM("ows?request=getCapabilities&service=wfs&version=1.1.0&guid="
                + GUID_ABC);
        // print(dom);
        assertEquals(2, getMatchCount("//wfs:FeatureType", dom));
        assertEquals(
                1,
                getMatchCount("//wfs:FeatureType[wfs:Name = '" + getLayerId(MockData.POLYGONS)
                        + "']", dom));
        assertEquals(
                1,
                getMatchCount("//wfs:FeatureType[wfs:Name = '" + getLayerId(MockData.LAKES) + "']",
                        dom));
    }

    private int getMatchCount(String path, Document dom) throws XpathException {
        return xpath.getMatchingNodes(path, dom).getLength();
    }

    @Test
    public void testGetFeatureHiddenLayer() throws Exception {
        dao.addRule(abcBuildingsMainStreet);

        Document dom = getAsDOM("ows?request=GetFeature&service=wfs&version=1.1.0&guid=" + GUID_ABC
                + "&typename=" + getLayerId(MockData.LAKES));
        checkOws10Exception(dom, ServiceException.INVALID_PARAMETER_VALUE, "typeName");
    }

    @Test
    public void testGetFeatureRestricted() throws Exception {
        dao.addRule(abcBuildingsMainStreet);

        Document dom = getAsDOM("ows?request=GetFeature&service=wfs&version=1.1.0&guid=" + GUID_ABC
                + "&typename=" + getLayerId(MockData.BUILDINGS));
        // print(dom);
        assertEquals(1, getMatchCount("//cite:Buildings", dom));
        assertEquals("123 Main Street", xpath.evaluate("//cite:Buildings/cite:ADDRESS", dom));
    }

}
