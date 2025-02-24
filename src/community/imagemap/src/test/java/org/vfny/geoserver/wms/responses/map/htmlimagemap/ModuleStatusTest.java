/*
 * (c) 2021 Open Source Geospatial Foundation - all rights reserved This code is licensed under the
 * GPL 2.0 license, available at the root application directory.
 */
package org.vfny.geoserver.wms.responses.map.htmlimagemap;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Optional;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.ModuleStatus;
import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class ModuleStatusTest {
    @Test
    public void test() {
        try (ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("applicationContext.xml")) {
            assertNotNull(context);

            Optional<ModuleStatus> status = GeoServerExtensions.extensions(ModuleStatus.class, context).stream()
                    .filter(s -> s.getModule().equalsIgnoreCase("gs-imagemap"))
                    .findFirst();
            assertTrue(status.isPresent());
        }
    }
    /*
     * public void test() { assertModuleStatus("gs-imagemap", "ImageMap Extension"); }
     */
}
