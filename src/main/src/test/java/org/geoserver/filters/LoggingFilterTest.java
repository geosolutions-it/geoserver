/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.filters;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.mock;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;
import javax.servlet.ServletException;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.SettingsInfoImpl;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class LoggingFilterTest {
    Logger logger;
    private static OutputStream logCapturingStream;
    private static StreamHandler customLogHandler;

    private static String expectedLogPart = "took";
    private static String expectedHeadersLogPart = "Headers:";
    private static String expectedBodyLogPart = "body:";

    @Before
    public void setup() {
        logger = Logger.getLogger("org.geoserver.filters");
        logger.setLevel(Level.INFO);
        logCapturingStream = new ByteArrayOutputStream();
        customLogHandler = new StreamHandler(logCapturingStream, new SimpleFormatter());
        logger.addHandler(customLogHandler);
    }

    @Test
    public void testRequestLoggingDoesNotOccur() throws IOException, ServletException {
        String capturedLog = getLog("false", "true", "true");
        assertFalse(capturedLog.contains(expectedLogPart));
        assertFalse(capturedLog.contains(expectedHeadersLogPart));
        assertFalse(capturedLog.contains(expectedBodyLogPart));
    }

    @Test
    public void testRequestLoggingBody() throws IOException, ServletException {
        String capturedLog = getLog("true", "true", "false");
        assertTrue(capturedLog.contains(expectedLogPart));
        assertFalse(capturedLog.contains(expectedHeadersLogPart));
        assertTrue(capturedLog.contains(expectedBodyLogPart));
    }

    @Test
    public void testRequestLoggingHeaders() throws IOException, ServletException {
        String capturedLog = getLog("true", "false", "true");
        assertTrue(capturedLog.contains(expectedLogPart));
        assertTrue(capturedLog.contains(expectedHeadersLogPart));
        assertFalse(capturedLog.contains(expectedBodyLogPart));
    }

    private String getTestCapturedLog() throws IOException {
        customLogHandler.flush();
        return logCapturingStream.toString();
    }

    private String getLog(String requestsEnabled, String bodiesEnabled, String headersEnabled)
            throws IOException, ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("PUT");
        MockHttpServletResponse response = new MockHttpServletResponse();
        GeoServer geoServer = new GeoServerImpl();
        GeoServerInfo geoServerInfo = mock(GeoServerInfoImpl.class);
        MetadataMap metadata = new MetadataMap();
        metadata.put(LoggingFilter.LOG_REQUESTS_ENABLED, requestsEnabled);
        metadata.put(LoggingFilter.LOG_BODIES_ENABLED, bodiesEnabled);
        metadata.put(LoggingFilter.LOG_HEADERS_ENABLED, headersEnabled);
        expect(geoServerInfo.getMetadata()).andReturn(metadata).anyTimes();
        expect(geoServerInfo.getClientProperties()).andReturn(new HashMap<>()).anyTimes();
        expect(geoServerInfo.getCoverageAccess())
                .andReturn(new CoverageAccessInfoImpl())
                .anyTimes();
        expect(geoServerInfo.getSettings()).andReturn(new SettingsInfoImpl()).anyTimes();
        replay(geoServerInfo);
        geoServer.setGlobal(geoServerInfo);
        LoggingFilter filter =
                new LoggingFilter(geoServer) {
                    public LoggingFilter setLogger(Logger logger) {
                        this.logger = logger;
                        return this;
                    }
                }.setLogger(logger);
        MockFilterChain chain = new MockFilterChain();
        filter.doFilter(request, response, chain);
        String capturedLog = getTestCapturedLog();
        return capturedLog;
    }
}
