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

    /**
     * @return
     * @throws IllegalArgumentException
     */
    protected XStream initialize() throws IllegalArgumentException {
        XStreamPersisterFactory xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStream xs = xpf.createXMLPersister().getXStream();

        // Aliases
        xs.alias("resources", Resources.class);
        xs.alias("resource", Resource.class);
        xs.aliasField("abstract", Resource.class, "abstractTxt");
        xs.aliasField("translateContext", Resource.class, "translateContext");
        xs.aliasAttribute(Resource.class, "type", "class");

        xs.alias("nativeBoundingBox", Map.class);

        xs.alias("defaultStyle", Map.class);
        xs.alias("metadata", Map.class);

        xs.alias("translateContext", TranslateContext.class);
        xs.alias("item", TranslateItem.class);
        xs.aliasAttribute(TranslateItem.class, "type", "class");
        xs.aliasAttribute(TranslateItem.class, "order", "order");

        // Converters
        xs.addImplicitCollection(Resources.class, "resources");
        xs.addImplicitCollection(TranslateContext.class, "items");

        xs.registerConverter(new MapEntryConverter());
        xs.registerConverter(new ResourceConverter(this.catalog));
        xs.registerConverter(new ResourceItemConverter());
        xs.registerConverter(new ReflectionConverter(xs.getMapper(),
                new PureJavaReflectionProvider()), XStream.PRIORITY_VERY_LOW);

        return xs;
    }
}
