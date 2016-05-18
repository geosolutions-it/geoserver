/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.geoserver.backuprestore.BackupRestoreTestSupport;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.platform.resource.Resource;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import net.sf.json.JSONObject;

/**
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RESTRestoreTest extends BackupRestoreTestSupport {

    @After
    public void cleanCatalog() throws IOException {
        for (StoreInfo s : getCatalog().getStores(StoreInfo.class)) {
            removeStore(s.getWorkspace().getName(), s.getName());
        }
        for (StyleInfo s : getCatalog().getStyles()) {
            String styleName = s.getName();
            if (!DEFAULT_STYLEs.contains(styleName)) {
                removeStyle(null, styleName);
            }
        }
    }
    
    @Test
    public void testNewRestore() throws Exception {
        Resource archiveFile = file("geoserver-alfa2-backup.zip");
        
        String json = 
                "{\"restore\": {" + 
                "   \"archiveFile\": \""+archiveFile.path()+"\" " +  
                "  }" + 
                "}";
        
        JSONObject execution = postNewRestore(json);

        assertTrue(execution.getLong("id") == 0);
        assertTrue("STARTED".equals(execution.getString("status")));

        while ("STARTED".equals(execution.getString("status"))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    JSONObject postNewRestore(String body) throws Exception {
        MockHttpServletResponse resp = body == null ? postAsServletResponse("/rest/br/restore", "")
                : postAsServletResponse("/rest/br/restore", body, "application/json");

        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));
        assertTrue(resp.getHeader("Location").matches(".*/restore/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject execution = json.getJSONObject("restore");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/br/restore/" + executionId);

        JSONObject execution = json.getJSONObject("restore");

        assertNotNull(execution);

        return execution;
    }
}
