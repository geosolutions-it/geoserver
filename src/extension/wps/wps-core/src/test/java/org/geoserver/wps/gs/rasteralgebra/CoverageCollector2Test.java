/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2011, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wps.gs.rasteralgebra;

import java.util.ArrayList;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.factory.GeoTools;
import org.junit.Assert;

/**
 * Testing {@link FilterCoverageCollector}.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class CoverageCollector2Test extends BaseRasterAlgebraTest {

public void testBasicTest() throws Exception {
    // instantiate collector
    final ListCoverageCollector collector = new ListCoverageCollector(catalog,
            ResolutionChoice.getDefault(), GeoTools.getDefaultHints());
    collector.collect(new ArrayList<String>() {
        {
            add("srtm_39_04_1");
            add("srtm_39_04_2");
        }
    });

    final Map<String, GridCoverage2D> coverages = collector.getCoverages();
    Assert.assertNotNull(coverages);
    Assert.assertEquals("Wrong number of coverages found:" + coverages.size(),
            2, coverages.size());
    Assert.assertNotNull("Null gridcoverage found: srtm_39_04_1",
            coverages.get("wcs:srtm_39_04_1"));
    Assert.assertNotNull("Null gridcoverage found: srtm_39_04_2",
            coverages.get("wcs:srtm_39_04_2"));
    Assert.assertNotNull("Null Gridgeometry found:",
            collector.getGridGeometry());

    collector.dispose();

}
}
