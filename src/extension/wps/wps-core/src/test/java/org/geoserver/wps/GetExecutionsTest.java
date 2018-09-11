/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.PRIMITIVEGEOFEATURE;

import java.util.HashMap;
import javax.xml.namespace.QName;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

/** @author Alessio Fabiani, GeoSolutions SAS */
public class GetExecutionsTest extends WPSTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        testData.addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, getCatalog());

        String pgf = PRIMITIVEGEOFEATURE.getLocalPart();
        testData.addVectorLayer(
                new QName("http://foo.org", pgf, "foo"),
                new HashMap<LayerProperty, Object>(),
                pgf + ".properties",
                MockData.class,
                getCatalog());
    }

    @Before
    public void oneTimeSetUp() throws Exception {
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);
        // want at least two asynchronous processes to test concurrency
        wps.setMaxAsynchronousProcesses(Math.max(2, wps.getMaxAsynchronousProcesses()));
        getGeoServer().save(wps);
    }

    @Before
    public void setUpInternal() throws Exception {
        // make extra sure we don't have anything else going
        MonkeyProcess.clearCommands();
    }

    /**
     * Tests a process execution with a BoudingBox as the output and check internal layer request
     * handling as well
     */
    @Test
    public void testBoundsProcess() throws Exception {
        String request =
                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                        + "<wps:Execute version=\"1.0.0\" service=\"WPS\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns=\"http://www.opengis.net/wps/1.0.0\" xmlns:wfs=\"http://www.opengis.net/wfs\" xmlns:wps=\"http://www.opengis.net/wps/1.0.0\" xmlns:ows=\"http://www.opengis.net/ows/1.1\" xmlns:gml=\"http://www.opengis.net/gml\" xmlns:ogc=\"http://www.opengis.net/ogc\" xmlns:wcs=\"http://www.opengis.net/wcs/1.1.1\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/wps/1.0.0 http://schemas.opengis.net/wps/1.0.0/wpsAll.xsd\">\n"
                        + "  <ows:Identifier>gs:Bounds</ows:Identifier>\n"
                        + "  <wps:DataInputs>\n"
                        + "    <wps:Input>\n"
                        + "      <ows:Identifier>features</ows:Identifier>\n"
                        + "      <wps:Reference mimeType=\"text/xml; subtype=wfs-collection/1.0\" xlink:href=\"http://geoserver/wfs\" method=\"POST\">\n"
                        + "        <wps:Body>\n"
                        + "          <wfs:GetFeature service=\"WFS\" version=\"1.0.0\">\n"
                        + "            <wfs:Query typeName=\"cite:Streams\"/>\n"
                        + "          </wfs:GetFeature>\n"
                        + "        </wps:Body>\n"
                        + "      </wps:Reference>\n"
                        + "    </wps:Input>\n"
                        + "  </wps:DataInputs>\n"
                        + "  <wps:ResponseForm>\n"
                        + "    <wps:RawDataOutput>\n"
                        + "      <ows:Identifier>bounds</ows:Identifier>\n"
                        + "    </wps:RawDataOutput>\n"
                        + "  </wps:ResponseForm>\n"
                        + "</wps:Execute>";

        Document dom = postAsDOM(root(), request);
        assertXpathEvaluatesTo("-4.0E-4 -0.0024", "/ows:BoundingBox/ows:LowerCorner", dom);
        assertXpathEvaluatesTo("0.0036 0.0024", "/ows:BoundingBox/ows:UpperCorner", dom);

        // Anonymous users do not have access to the executions list
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo(
                "No Process Execution available.",
                "/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                dom);

        // As an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "gs:Bounds",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Process/ows:Identifier",
                dom);
        assertXpathEvaluatesTo(
                "gs:Bounds",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:Identifier",
                dom);
        assertXpathEvaluatesTo(
                "SUCCEEDED",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:Status",
                dom);
        assertXpathEvaluatesTo(
                "100.0",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:PercentCompleted",
                dom);

        // As a system user I can access only my own processes
        login("afabiani", "geosolutions", "ROLE_AUTHENITCATED");
        dom = postAsDOM(root(), request);
        // print(dom);
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "gs:Bounds",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Process/ows:Identifier",
                dom);
        assertXpathEvaluatesTo(
                "afabiani",
                "/wps:GetExecutionsResponse/wps:ExecuteResponse/wps:Status/wps:Owner",
                dom);

        // Again as an Admin I have access to the whole executions list
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        // print(dom);
        assertXpathEvaluatesTo("2", "/wps:GetExecutionsResponse/@count", dom);

        // Unless I filter out only the ones belonging to some user
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions&owner=afabiani");
        // print(dom);
        assertXpathEvaluatesTo("1", "/wps:GetExecutionsResponse/@count", dom);

        // Let's do some simple pagination tests now...
        for (int i = 0; i < 3; i++) {
            dom = postAsDOM(root(), request);
        }
        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions");
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);

        dom = getAsDOM(root() + "service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2");
        // print(dom);
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo("", "/wps:GetExecutionsResponse/@previous", dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=2",
                "/wps:GetExecutionsResponse/@next",
                dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&startIndex=1&maxFeatures=2");
        // print(dom);
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=0",
                "/wps:GetExecutionsResponse/@previous",
                dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=3",
                "/wps:GetExecutionsResponse/@next",
                dom);

        dom =
                getAsDOM(
                        root()
                                + "service=wps&version=1.0.0&request=GetExecutions&startIndex=3&maxFeatures=2");
        // print(dom);
        assertXpathEvaluatesTo("5", "/wps:GetExecutionsResponse/@count", dom);
        assertXpathEvaluatesTo(
                "/ows?service=wps&version=1.0.0&request=GetExecutions&maxFeatures=2&startIndex=1",
                "/wps:GetExecutionsResponse/@previous",
                dom);
        assertXpathEvaluatesTo("", "/wps:GetExecutionsResponse/@next", dom);
    }
}
