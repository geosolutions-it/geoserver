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
        String json = 
                "{" + 
                    "\"backup\": { " + 
                        "\"parameters\": " +
                            "\"{output.file.path=file://F:/tmp/xml/outputs/, time=1462286275654}\"" +
                    "}" + 
                "}";
        int i = postNewBackup(json);
        
        assertTrue(i == 0);
        
        // TODO: Read Backup State
    }
    
    
    int postNewBackup() throws Exception {
        return postNewBackup(null);
    }
    
    int postNewBackup(String body) throws Exception {
        MockHttpServletResponse resp = body == null ? postAsServletResponse("/rest/br/backup", "")
            : postAsServletResponse("/rest/br/backup", body, "application/json");
        
        assertEquals(201, resp.getStatus());
        assertNotNull( resp.getHeader( "Location") );
        assertTrue(resp.getHeader("Location").matches(".*/backup/\\d"));
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject executionId = json.getJSONObject("backup");
        return executionId.getInt("id");
    }
}
