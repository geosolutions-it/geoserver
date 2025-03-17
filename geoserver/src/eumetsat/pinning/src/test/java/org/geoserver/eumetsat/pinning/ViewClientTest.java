package org.geoserver.eumetsat.pinning;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.geoserver.eumetsat.pinning.views.View;
import org.geoserver.eumetsat.pinning.views.ViewsClient;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class ViewClientTest extends GeoServerSystemTestSupport {

    private static WireMockServer wireMockServer;

    @Before
    public void setup() {
        wireMockServer = new WireMockServer(8081); // Mock server on port 8081
        wireMockServer.start();
        WireMock.configureFor("localhost", 8081);
    }

    @After
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void testFetchViews() throws Exception {
        // Change this once done
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        Path path = Paths.get(classLoader.getResource("response.json").toURI());
        String jsonResponse = Files.readString(path);

        // Configure WireMock to return JSON when the endpoint is hit
        wireMockServer.stubFor(
                WireMock.get(urlEqualTo("/userPreferences/preferences"))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "application/json")
                                        .withBody(jsonResponse)));

        // Create ViewsClient with WireMock's base URL
        ViewsClient viewsClient = new ViewsClient(null, null);

        // Fetch and assert
        List<View> views = viewsClient.fetchViews(null);
        assert !views.isEmpty();
        assert views.get(0).getViewId() == 1052L;
    }
}
