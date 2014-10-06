/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.junit.Test;

import com.thoughtworks.xstream.XStream;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class ResourceLoaderProcessTest extends WPSResourceTestSupport {

    @Test
    public void testResourceLoaderProcess() {

        // Initialize Unmarshaller
        XStream xs = initialize();

        // De-serialize resources
        File test2;
        try {
            test2 = new File(testData.getDataDirectoryRoot().getAbsolutePath(), "test2.xml");
            assertTrue(test2.exists());

            String dump = null;
            FileInputStream fis = new FileInputStream(test2);
            try {
                dump = IOUtils.toString(fis, "UTF-8");
            } finally {
                IOUtils.closeQuietly(fis);
            }

            ResourceLoaderProcess rsp = new ResourceLoaderProcess(getGeoServer(),
                    (WPSResourceManager) applicationContext.getBean("wpsResourceManager"));
            rsp.execute(dump, null);

            LayerInfo layer = getGeoServer().getCatalog().getLayerByName("way_points");
            assertNotNull(layer);
        } catch (Exception cause) {
            fail(cause.getMessage());
            throw new ProcessException(cause);
        }
    }

}
