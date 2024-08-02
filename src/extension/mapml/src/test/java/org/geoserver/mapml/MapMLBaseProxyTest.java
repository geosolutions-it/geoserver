/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.junit.Assert.assertTrue;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.util.Map;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.junit.AfterClass;
import org.junit.Before;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.w3c.dom.Document;

public class MapMLBaseProxyTest extends MapMLTestSupport {

    protected static String CAPABILITIES_URL;

    protected static String MOCK_SERVER;

    protected static String CONTEXT;

    protected static String PATH;

    protected static void initMockService(String server, String context, String path, String file) {
        MOCK_SERVER = server;
        CONTEXT = context;
        PATH = path;
        CAPABILITIES_URL = MOCK_SERVER + CONTEXT + "?" + PATH;
        WireMockConfiguration config = wireMockConfig().dynamicPort();
        mockService = new WireMockServer(config);
        mockService.start();
        mockService.stubFor(
                WireMock.get(urlEqualTo(CAPABILITIES_URL))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withHeader("Content-Type", MediaType.TEXT_XML_VALUE)
                                        .withBodyFile(file)));
    }

    public static String getCapabilitiesURL() {
        return CAPABILITIES_URL;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        mockService.shutdown();
    }

    protected static final String BASE_REQUEST =
            "wms?LAYERS=cascadedLayer"
                    + "&STYLES=&FORMAT="
                    + MapMLConstants.MAPML_MIME_TYPE
                    + "&SERVICE=WMS&VERSION=1.3.0"
                    + "&REQUEST=GetMap"
                    + "&SRS=EPSG:4326"
                    + "&BBOX=0,0,1,1"
                    + "&WIDTH=150"
                    + "&HEIGHT=150"
                    + "&format_options="
                    + MapMLConstants.MAPML_WMS_MIME_TYPE_OPTION
                    + ":image/png";

    protected XpathEngine xpath;

    protected static WireMockServer mockService;

    @Override
    protected void registerNamespaces(Map<String, String> namespaces) {
        namespaces.put("wms", "http://www.opengis.net/wms");
        namespaces.put("ows", "http://www.opengis.net/ows");
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
    }

    @Before
    public void setup() {
        xpath = XMLUnit.newXpathEngine();
    }

    protected void checkCascading(String path, boolean shouldCascade, String rel) throws Exception {
        Document doc = getMapML(path);
        // printDocument(doc);
        String url = xpath.evaluate("//html:map-link[@rel='" + rel + "']/@tref", doc);
        assertCascading(shouldCascade, url);

        url = xpath.evaluate("//html:map-link[@rel='query']/@tref", doc);
        assertCascading(shouldCascade, url);
    }

    protected void assertCascading(boolean shouldCascade, String url) {
        if (shouldCascade) {
            assertTrue(
                    url.startsWith(
                            "http://localhost:" + mockService.port() + MOCK_SERVER + CONTEXT));
            assertTrue(url.contains("layers=topp:states"));
        } else {
            assertTrue(url.startsWith("http://localhost:8080/geoserver" + CONTEXT));
            assertTrue(url.contains("layers=cascadedLayer"));
        }
    }

    /**
     * Executes a request using the GET method and returns the result as an MapML document.
     *
     * @param path The portion of the request after the context, example:
     * @return A result of the request parsed into a dom.
     */
    protected org.w3c.dom.Document getMapML(final String path) throws Exception {
        MockHttpServletRequest request = createRequest(path, false);
        request.addHeader("Accept", "text/mapml");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        String resp = dispatch(request, "UTF-8").getContentAsString();
        return dom(new ByteArrayInputStream(resp.getBytes()), true);
    }

    /** For debugging purposes */
    public static void printDocument(Document doc) throws TransformerException {
        // Initialize a transformer
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        // Set output properties for the transformer (optional, for pretty print)
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        // Convert the DOM Document to a String
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        DOMSource source = new DOMSource(doc);
        transformer.transform(source, result);

        // Return the XML string
        // System.out.println(writer.toString());
    }
}
