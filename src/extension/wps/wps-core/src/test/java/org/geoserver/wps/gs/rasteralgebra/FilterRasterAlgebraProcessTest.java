/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.rasteralgebra;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.junit.Assert;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * @author Simone Giannecchini, GeoSolutions
 */
public class FilterRasterAlgebraProcessTest extends BaseRasterAlgebraTest {

@Test
public void testOperation1() throws Exception {
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraNotLessThan.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected image/tiff",
            response.getContentType(), "image/tiff");

    final File output = File.createTempFile("algebra", "tif", new File(
            "./target"));
    FileUtils.writeByteArrayToFile(output, getBinary(response));

    GeoTiffFormat format = new GeoTiffFormat();
    Assert.assertTrue("GeoTiff format unable to parse this file",
            format.accepts(output));
    GeoTiffReader reader = format.getReader(output);
    GridCoverage2D gc = reader.read(null);
    Assert.assertNotNull("Unable to read this coverage", gc);
    Assert.assertTrue(CRS.equalsIgnoreMetadata(
            gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
    Assert.assertEquals(10.0, gc.getEnvelope().getMinimum(0), 1E-6);
    Assert.assertEquals(41.0, gc.getEnvelope().getMinimum(1), 1E-6);
    Assert.assertEquals(13.0, gc.getEnvelope().getMaximum(0), 1E-6);
    Assert.assertEquals(45.0, gc.getEnvelope().getMaximum(1), 1E-6);

    testBinaryGC(gc);

    scheduleForDisposal(gc);
    reader.dispose();

}

@Test
public void testOperation2() throws Exception {
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraAnd.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected image/tiff",
            response.getContentType(), "image/tiff");

    final File output = File.createTempFile("algebra", "tif", new File(
            "./target"));
    FileUtils.writeByteArrayToFile(output, getBinary(response));

    GeoTiffFormat format = new GeoTiffFormat();
    Assert.assertTrue("GeoTiff format unable to parse this file",
            format.accepts(output));
    GeoTiffReader reader = format.getReader(output);
    GridCoverage2D gc = reader.read(null);
    Assert.assertNotNull("Unable to read this coverage", gc);
    Assert.assertTrue(CRS.equalsIgnoreMetadata(
            gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
    Assert.assertEquals(12.0, gc.getEnvelope().getMinimum(0), 1E-6);
    Assert.assertEquals(42.0, gc.getEnvelope().getMinimum(1), 1E-6);
    Assert.assertEquals(13.0, gc.getEnvelope().getMaximum(0), 1E-6);
    Assert.assertEquals(44.0, gc.getEnvelope().getMaximum(1), 1E-6);

    testBinaryGC(gc);

    scheduleForDisposal(gc);
    reader.dispose();
}

@Test
public void testMax() throws Exception {
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraMax.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected image/tiff",
            response.getContentType(), "image/tiff");

    final File output = File.createTempFile("algebra", "tif", new File(
            "./target"));
    FileUtils.writeByteArrayToFile(output, getBinary(response));

    GeoTiffFormat format = new GeoTiffFormat();
    Assert.assertTrue("GeoTiff format unable to parse this file",
            format.accepts(output));
    GeoTiffReader reader = format.getReader(output);
    GridCoverage2D gc = reader.read(null);
    Assert.assertNotNull("Unable to read this coverage", gc);
    Assert.assertTrue(CRS.equalsIgnoreMetadata(
            gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
    Assert.assertEquals(12.0, gc.getEnvelope().getMinimum(0), 1E-6);
    Assert.assertEquals(42.0, gc.getEnvelope().getMinimum(1), 1E-6);
    Assert.assertEquals(13.0, gc.getEnvelope().getMaximum(0), 1E-6);
    Assert.assertEquals(44.0, gc.getEnvelope().getMaximum(1), 1E-6);

    // check values
    final ImageWorker worker = new ImageWorker(gc.getRenderedImage());
    // check values
    final double[] maximum = worker.getMaximums();
    Assert.assertNotNull(maximum);
    Assert.assertEquals(1, maximum.length);
    Assert.assertEquals(2049.0, maximum[0], 1E-6);
    final double[] minimum = worker.getMinimums();
    Assert.assertNotNull(minimum);
    Assert.assertEquals(1, minimum.length);
    Assert.assertEquals(-32768.0, minimum[0], 1E-6);

    scheduleForDisposal(gc);
    reader.dispose();
}

@Test
public void testOperationComplex1() throws Exception {
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraComplex1.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected image/tiff",
            response.getContentType(), "image/tiff");

    final File output = File.createTempFile("algebra", "tif", new File(
            "./target"));
    FileUtils.writeByteArrayToFile(output, getBinary(response));

    GeoTiffFormat format = new GeoTiffFormat();
    Assert.assertTrue("GeoTiff format unable to parse this file",
            format.accepts(output));
    GeoTiffReader reader = format.getReader(output);
    GridCoverage2D gc = reader.read(null);
    Assert.assertNotNull("Unable to read this coverage", gc);
    Assert.assertTrue(CRS.equalsIgnoreMetadata(
            gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
    Assert.assertEquals(12.0, gc.getEnvelope().getMinimum(0), 1E-6);
    Assert.assertEquals(43.0, gc.getEnvelope().getMinimum(1), 1E-6);
    Assert.assertEquals(13.0, gc.getEnvelope().getMaximum(0), 1E-6);
    Assert.assertEquals(44.0, gc.getEnvelope().getMaximum(1), 1E-6);

    testBinaryGC(gc);

    scheduleForDisposal(gc);
    reader.dispose();
}

@Test
public void testOperationComplex2() throws Exception {
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraComplex2.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected image/tiff",
            response.getContentType(), "image/tiff");

    final File output = File.createTempFile("algebra", "tif", new File(
            "./target"));
    FileUtils.writeByteArrayToFile(output, getBinary(response));

    GeoTiffFormat format = new GeoTiffFormat();
    Assert.assertTrue("GeoTiff format unable to parse this file",
            format.accepts(output));
    GeoTiffReader reader = format.getReader(output);
    GridCoverage2D gc = reader.read(null);
    Assert.assertNotNull("Unable to read this coverage", gc);
    Assert.assertTrue(CRS.equalsIgnoreMetadata(
            gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
    Assert.assertEquals(13.0, gc.getEnvelope().getMinimum(0), 1E-6);
    Assert.assertEquals(42.0, gc.getEnvelope().getMinimum(1), 1E-6);
    Assert.assertEquals(15.0, gc.getEnvelope().getMaximum(0), 1E-6);
    Assert.assertEquals(44.0, gc.getEnvelope().getMaximum(1), 1E-6);

    Assert.assertEquals(0.013888888888888892, XAffineTransform
            .getScaleX0((AffineTransform) gc.getGridGeometry().getGridToCRS()),
            1E-6);
    Assert.assertEquals(0.013888888888888892, XAffineTransform
            .getScaleY0((AffineTransform) gc.getGridGeometry().getGridToCRS()),
            1E-6);

    testBinaryGC(gc);

    scheduleForDisposal(gc);
    reader.dispose();
}

@Test
public void testOperationComplex3() throws Exception {
    // roi does not intersect coverages
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraComplex3.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected text/xml",
            response.getContentType(), "text/xml");
}

@Test
public void testOperationComplex4() throws Exception {
    String xml = FileUtils.readFileToString(new File(
            "./src/test/resources/rasteralgebraComplex4.xml"));

    MockHttpServletResponse response = postAsServletResponse(root(), xml);
    Assert.assertEquals("Wrong mime type, expected image/tiff",
            response.getContentType(), "image/tiff");

    final File output = File.createTempFile("algebra", "tif", new File(
            "./target"));
    FileUtils.writeByteArrayToFile(output, getBinary(response));

    GeoTiffFormat format = new GeoTiffFormat();
    Assert.assertTrue("GeoTiff format unable to parse this file",
            format.accepts(output));
    GeoTiffReader reader = format.getReader(output);
    GridCoverage2D gc = reader.read(null);
    Assert.assertNotNull("Unable to read this coverage", gc);
    Assert.assertTrue(CRS.equalsIgnoreMetadata(
            gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
    Assert.assertEquals(13.0, gc.getEnvelope().getMinimum(0), 1E-6);
    Assert.assertEquals(42.0, gc.getEnvelope().getMinimum(1), 1E-6);
    Assert.assertEquals(15.0, gc.getEnvelope().getMaximum(0), 1E-6);
    Assert.assertEquals(44.0, gc.getEnvelope().getMaximum(1), 1E-6);

    Assert.assertEquals(0.016666666666666666, XAffineTransform
            .getScaleX0((AffineTransform) gc.getGridGeometry().getGridToCRS()),
            1E-6);
    Assert.assertEquals(0.016666666666666666, XAffineTransform
            .getScaleY0((AffineTransform) gc.getGridGeometry().getGridToCRS()),
            1E-6);

    testBinaryGC(gc);

    scheduleForDisposal(gc);
    reader.dispose();
}
}
