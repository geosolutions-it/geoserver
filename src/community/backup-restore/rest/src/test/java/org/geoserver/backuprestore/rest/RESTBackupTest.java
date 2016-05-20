/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.backuprestore.BackupRestoreTestSupport;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import net.sf.json.JSONObject;

/**
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RESTBackupTest extends BackupRestoreTestSupport {
    @Test
    public void testNewBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");
        
        String json = 
                "{\"backup\": {" + 
                "   \"archiveFile\": \""+archiveFilePath+"\", " + 
                "   \"overwrite\": true," + 
                "   \"options\": [\"BK_BEST_EFFORT\"]" +
                "  }" + 
                "}";
        
        JSONObject execution = postNewBackup(json);

        assertTrue("STARTED".equals(execution.getString("status")));

        while ("STARTED".equals(execution.getString("status"))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    JSONObject postNewBackup(String body) throws Exception {
        MockHttpServletResponse resp = body == null ? postAsServletResponse("/rest/br/backup", "")
                : postAsServletResponse("/rest/br/backup", body, "application/json");

        assertEquals(201, resp.getStatus());
        assertNotNull(resp.getHeader("Location"));
        assertTrue(resp.getHeader("Location").matches(".*/backup/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject execution = json.getJSONObject("backup");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/br/backup/" + executionId);

        JSONObject execution = json.getJSONObject("backup");

        assertNotNull(execution);

        return execution;
    }
}
