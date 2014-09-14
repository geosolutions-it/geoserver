/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.rasteralgebra;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.junit.Assert;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * @author Simone Giannecchini, GeoSolutions
 */
public class JiffleProcessTest extends BaseRasterAlgebraTest {

    @Test
    public void testUnary() throws Exception {
        String xml = FileUtils.readFileToString(new File("./src/test/resources/jiffle_unary.xml"));

        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        Assert.assertEquals("Wrong mime type, expected image/tiff",response.getContentType(), "image/tiff");


        final File output = File.createTempFile("algebra", ".tif", new File("./target"));
        FileUtils.writeByteArrayToFile(output,getBinary(response));
        
        GeoTiffFormat format = new GeoTiffFormat();
        Assert.assertTrue("GeoTiff format unable to parse this file",format.accepts(output));
        GeoTiffReader reader = format.getReader(output);
        GridCoverage2D gc = reader.read(null);
        Assert.assertNotNull("Unable to read this coverage",gc);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
        Assert.assertEquals(10.0, gc.getEnvelope().getMinimum(0),1E-6);
        Assert.assertEquals(40.0, gc.getEnvelope().getMinimum(1),1E-6);
        Assert.assertEquals(15.0, gc.getEnvelope().getMaximum(0),1E-6);
        Assert.assertEquals(45.0, gc.getEnvelope().getMaximum(1),1E-6);

        testMaxImage(gc.getRenderedImage());
        
        scheduleForDisposal(gc);
        reader.dispose();
    }

    @Test
    public void testBinary() throws Exception {
        String xml = FileUtils.readFileToString(new File("./src/test/resources/jiffle_binary1.xml"));
    
        MockHttpServletResponse response = postAsServletResponse(root(), xml);
        Assert.assertEquals("Wrong mime type, expected image/tiff", "image/tiff",response.getContentType());
    
    
        final File output = File.createTempFile("algebra", ".tif", new File("./target"));
        FileUtils.writeByteArrayToFile(output,getBinary(response));
        
        GeoTiffFormat format = new GeoTiffFormat();
        Assert.assertTrue("GeoTiff format unable to parse this file",format.accepts(output));
        GeoTiffReader reader = format.getReader(output);
        GridCoverage2D gc = reader.read(null);
        Assert.assertNotNull("Unable to read this coverage",gc);
        Assert.assertTrue(CRS.equalsIgnoreMetadata(gc.getCoordinateReferenceSystem(), CRS.decode("EPSG:4326")));
        Assert.assertEquals(11.0, gc.getEnvelope().getMinimum(0),1E-6);
        Assert.assertEquals(41.0, gc.getEnvelope().getMinimum(1),1E-6);
        Assert.assertEquals(13.0, gc.getEnvelope().getMaximum(0),1E-6);
        Assert.assertEquals(44.0, gc.getEnvelope().getMaximum(1),1E-6);
    
        // check values
        final ImageWorker worker = new ImageWorker(gc.getRenderedImage());
        final double[] maximum = worker.getMaximums();
        Assert.assertNotNull(maximum);
        Assert.assertEquals(1, maximum.length);
        Assert.assertEquals(1024.5, maximum[0],1E-6);
        final double[] minimum = worker.getMinimums();
        Assert.assertNotNull(minimum);
        Assert.assertEquals(1, minimum.length);
        Assert.assertEquals(-16384.0, minimum[0],1E-6);
        
        scheduleForDisposal(gc);
        reader.dispose();
    }
}