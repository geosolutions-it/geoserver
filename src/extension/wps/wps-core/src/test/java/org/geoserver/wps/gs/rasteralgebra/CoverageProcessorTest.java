/*
 *    GeoTools - The Open Source Java GIS
 Toolkit
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

import java.awt.image.RenderedImage;
import java.io.File;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.geotools.factory.GeoTools;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.image.ImageWorker;
import org.geotools.resources.image.ImageUtilities;
import org.junit.Assert;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;

/**
 * Testing the {@link CoverageProcessor} collector.
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class CoverageProcessorTest extends BaseRasterAlgebraTest {

private final Logger LOGGER = org.geotools.util.logging.Logging
        .getLogger(getClass());

public void testFilters() throws Exception {
    final File directory = new File("./src/test/resources");
    final String[] files = directory.list(FileFilterUtils.and(
            FileFilterUtils.suffixFileFilter("xml"), new IOFileFilter() {

                @Override
                public boolean accept(File arg0, String arg1) {
                    try {
                        return getFilter(arg0.getAbsolutePath()
                                + File.separator + arg1) instanceof Filter;
                    } catch (Exception e) {
                        return false;
                    }
                }

                @Override
                public boolean accept(File arg0) {
                    try {
                        return getFilter(arg0.getAbsolutePath()) instanceof Filter;
                    } catch (Exception e) {
                        return false;
                    }
                }
            }));
    // real testing
    Assert.assertNotNull(files);
    for (String file : files) {
        LOGGER.info("Testing filter " + file);
        testFilter("./src/test/resources" + File.separator + file);
        LOGGER.info("Testing filter " + file + " --> Ok");
    }
}

/**
 * Testing the filter at the provided path
 * 
 * @param filterPath
 * @throws Exception
 */
private void testFilter(String filterPath) throws Exception {
    final Filter filter = getFilter(filterPath);
    Assert.assertNotNull(filter);

    // instantiate collector
    final CoverageCollector collector = new CoverageCollector(catalog,
            ResolutionChoice.getDefault(), GeoTools.getDefaultHints());
    filter.accept(collector, null);

    // instantiate processor
    final CoverageProcessor processor = new CoverageProcessor(
            collector.getCoverages(), collector.getGridGeometry(),
            GeoTools.getDefaultHints());
    final RenderedImage result = testProcessor(filter, processor);
    Assert.assertNotNull(result);
    testBinaryImage(result);

    // dispose
    collector.dispose();
    processor.dispose();
    ImageUtilities.disposePlanarImageChain(PlanarImage
            .wrapRenderedImage(result));
}

/**
 * Deep testing of {@link CoverageProcessor};
 * 
 * @param filter
 * @param processor
 * @return
 */
private RenderedImage testProcessor(final Filter filter,
        final CoverageProcessor processor) {
    Object result_ = filter.accept(processor, null);
    Assert.assertNotNull(result_);
    Assert.assertTrue(result_ instanceof RenderedImage);
    RenderedImage result = (RenderedImage) result_;
    PlanarImage.wrapRenderedImage(result).getTiles();

    // check values
    testBinaryImage(result);
    return result;
}

public void testMax() throws Exception {

    Expression function = CQL
            .toExpression("max2(wcs:srtm_39_04_1,wcs:srtm_39_04_2,wcs:srtm_39_04_3,wcs:srtm_39_04)");
    // instantiate collector
    final CoverageCollector collector = new CoverageCollector(catalog,
            ResolutionChoice.getDefault(), GeoTools.getDefaultHints());
    function.accept(collector, null);

    // instantiate processor
    final CoverageProcessor processor = new CoverageProcessor(
            collector.getCoverages(), collector.getGridGeometry(),
            GeoTools.getDefaultHints());

    Object result_ = function.accept(processor, null);
    Assert.assertNotNull(result_);
    Assert.assertTrue(result_ instanceof RenderedImage);
    RenderedImage result = (RenderedImage) result_;
    PlanarImage.wrapRenderedImage(result).getTiles();

    // check values
    final ImageWorker worker = new ImageWorker(result);
    // check values
    final double[] maximum = worker.getMaximums();
    Assert.assertNotNull(maximum);
    Assert.assertEquals(1, maximum.length);
    Assert.assertEquals(2049.0, maximum[0], 1E-6);
    final double[] minimum = worker.getMinimums();
    Assert.assertNotNull(minimum);
    Assert.assertEquals(1, minimum.length);
    Assert.assertEquals(-32768.0, minimum[0], 1E-6);

    // dispose
    collector.dispose();
    processor.dispose();
    ImageUtilities.disposePlanarImageChain(PlanarImage
            .wrapRenderedImage(result));
}

public void testMin() throws Exception {

    Expression function = CQL
            .toExpression("min2(wcs:srtm_39_04_1,wcs:srtm_39_04_2,wcs:srtm_39_04_3,wcs:srtm_39_04)");
    // instantiate collector
    final CoverageCollector collector = new CoverageCollector(catalog,
            ResolutionChoice.getDefault(), GeoTools.getDefaultHints());
    function.accept(collector, null);

    // instantiate processor
    final CoverageProcessor processor = new CoverageProcessor(
            collector.getCoverages(), collector.getGridGeometry(),
            GeoTools.getDefaultHints());

    Object result_ = function.accept(processor, null);
    Assert.assertNotNull(result_);
    Assert.assertTrue(result_ instanceof RenderedImage);
    RenderedImage result = (RenderedImage) result_;
    PlanarImage.wrapRenderedImage(result).getTiles();

    // check values
    testMinImage(result);

    // dispose
    collector.dispose();
    processor.dispose();
    ImageUtilities.disposePlanarImageChain(PlanarImage
            .wrapRenderedImage(result));
}

public void testAbs() throws Exception {

    Expression function = CQL.toExpression("abs(wcs:srtm_39_04_1)");
    // instantiate collector
    final CoverageCollector collector = new CoverageCollector(catalog,
            ResolutionChoice.getDefault(), GeoTools.getDefaultHints());
    function.accept(collector, null);

    // instantiate processor
    final CoverageProcessor processor = new CoverageProcessor(
            collector.getCoverages(), collector.getGridGeometry(),
            GeoTools.getDefaultHints());

    Object result_ = function.accept(processor, null);
    Assert.assertNotNull(result_);
    Assert.assertTrue(result_ instanceof RenderedImage);
    RenderedImage result = (RenderedImage) result_;
    PlanarImage.wrapRenderedImage(result).getTiles();

    // check values
    testAbsImage(result);

    // dispose
    collector.dispose();
    processor.dispose();
    ImageUtilities.disposePlanarImageChain(PlanarImage
            .wrapRenderedImage(result));
}

public void testExp() throws Exception {

    Expression function = CQL.toExpression("exp(wcs:srtm_39_04_1)");
    // instantiate collector
    final CoverageCollector collector = new CoverageCollector(catalog,
            ResolutionChoice.getDefault(), GeoTools.getDefaultHints());
    function.accept(collector, null);

    // instantiate processor
    final CoverageProcessor processor = new CoverageProcessor(
            collector.getCoverages(), collector.getGridGeometry(),
            GeoTools.getDefaultHints());

    Object result_ = function.accept(processor, null);
    Assert.assertNotNull(result_);
    Assert.assertTrue(result_ instanceof RenderedImage);
    RenderedImage result = (RenderedImage) result_;
    PlanarImage.wrapRenderedImage(result).getTiles();

    // check values
    testExpImage(result);

    // dispose
    collector.dispose();
    processor.dispose();
    ImageUtilities.disposePlanarImageChain(PlanarImage
            .wrapRenderedImage(result));
}
}
