/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import java.io.IOException;
import java.util.Map;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.WPSTestSupport;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess.MapEntryConverter;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess.ResourceConverter;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess.ResourceItemConverter;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.Resources;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;

/**
 * 
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public abstract class WPSResourceTestSupport extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        // add limits properties file
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/test1.xml"), "test1.xml");
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/test2.xml"), "test2.xml");
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/test3.xml"), "test3.xml");
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/test4.xml"), "test4.xml");

        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/tracks_filtered.csv"), "tracks_filtered.csv");
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/tracks_filtered.prj"), "tracks_filtered.prj");
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/waypoints.csv"), "waypoints.csv");
        testData.copyTo(
                WPSResourceTestSupport.class.getClassLoader().getResourceAsStream(
                        "test-data/waypoints.prj"), "waypoints.prj");
    }

    protected void cleanCatalog() throws IOException {
        for (StoreInfo s : getGeoServer().getCatalog().getStores(StoreInfo.class)) {
            removeStore(null, s.getName());
        }
    }

}
