/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.junit.Test;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class ResourceLoaderProcessTest extends WPSResourceTestSupport {

    @Test
    public void testResourceLoaderProcess() {

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
            rsp.execute(new StringRawData(dump, "application/xml"), null);

            List<LayerInfo> layers = getGeoServer().getCatalog().getLayers();
            assertNotNull(layers);
            
            assertTrue(!layers.isEmpty());

            // cleanup
            cleanCatalog();
        } catch (Exception cause) {
            fail(cause.getMessage());
            throw new ProcessException(cause);
        }

        // De-serialize resources
        File test3;
        try {
            test3 = new File(testData.getDataDirectoryRoot().getAbsolutePath(), "test3.xml");
            assertTrue(test3.exists());

            String dump = null;
            FileInputStream fis = new FileInputStream(test3);
            try {
                dump = IOUtils.toString(fis, "UTF-8");
            } finally {
                IOUtils.closeQuietly(fis);
            }

            List<LayerInfo> layers = getGeoServer().getCatalog().getLayers();
            final int numLayers = layers.size();
            assertTrue(numLayers == 0);

            ResourceLoaderProcess rsp = new ResourceLoaderProcess(getGeoServer(),
                    (WPSResourceManager) applicationContext.getBean("wpsResourceManager"));
            rsp.execute(new StringRawData(dump, "application/xml"), null);

            assertNotNull(layers.size() > numLayers);

            // cleanup
            cleanCatalog();
        } catch (Exception cause) {
            fail(cause.getMessage());
            throw new ProcessException(cause);
        }
    }

}
