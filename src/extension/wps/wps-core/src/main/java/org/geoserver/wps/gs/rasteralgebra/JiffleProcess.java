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
/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.rasteralgebra;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRenderedImage;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.TiledImage;

import org.geoserver.catalog.Catalog;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.gs.GridCoverage2DRIA;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.resources.image.ImageUtilities;
import org.geotools.util.logging.Logging;
import org.jaitools.imageutils.ImageLayout2;
import org.jaitools.jiffle.JiffleBuilder;
import org.jaitools.jiffle.runtime.JiffleProgressListener;
import org.jaitools.jiffle.runtime.NullProgressListener;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A process that returns a coverage fully (something which is un-necessarily hard in WCS)
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */
@DescribeProcess(title = "JiffleProcess", description = "Compute the operations specified by the Jiffle script provided on the coverages provided")
public class JiffleProcess implements GeoServerProcess {
    
    private final static Logger LOGGER=Logging.getLogger(JiffleProcess.class);
        
    private Catalog catalog;
    
    public JiffleProcess(Catalog catalog) {
        this.catalog = catalog;
    }

    /**
     * @param classes representing the domain of the classes (Mandatory, not empty)
     * @param rasterT0 that is the reference Image (Mandatory)
     * @param rasterT1 rasterT1 that is the update situation (Mandatory)
     * @param roi that identifies the optional ROI (so that could be null)
     * @return
     */
    @DescribeResult(name = "JiffleProcess", description = "JiffleProcess", type=GridCoverage2D.class)
    public GridCoverage2D execute(
            @DescribeParameter(name = "script", description = "Jiffle Script to use on the raster data (mandatory)", min = 1) String script,
            @DescribeParameter(name = "ROI", min = 0, description = "Region Of Interest (optional)") Geometry roi,
            @DescribeParameter(name = "inputs", min = 1, description = "List of names for the input GridCoverages",collectionType=String.class) List<String> inputs,
            @DescribeParameter(name = "ResolutionChoice", min = 0, description = "How to choose the final resolution (optional, default goes to minimum") ResolutionChoice resolutionChoice)
            throws Exception {
      
        // === instantiate collector
        resolutionChoice=resolutionChoice!=null?resolutionChoice:ResolutionChoice.getDefault();
        
        // hints for tiling
        final Hints hints = GeoTools.getDefaultHints().clone();
        final ImageLayout2 layout = new ImageLayout2();
        layout.setTileWidth(JAI.getDefaultTileSize().width);
        layout.setTileHeight(JAI.getDefaultTileSize().height);
        hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout));
        
        // collect input coverages
        final ListCoverageCollector collector= new ListCoverageCollector(
                catalog,
                resolutionChoice,
                roi,
                hints);
        collector.collect(inputs);

        // create jiffle script
        final JiffleBuilder jb = new JiffleBuilder();
        // We pass the script to the builder and associate the source images 
        // with the variable names. Note the use of method chaining.
        jb.script(script);
        
        // set sources
        final Map<String, GridCoverage2D> coverages = collector.getCoverages();
        final Map<String, GridCoverage2DRIA> rias= new HashMap<String, GridCoverage2DRIA>();
        try{
            int k=1;
            final GridGeometry2D destGridGeometry = collector.getGridGeometry();
            for(Map.Entry<String, GridCoverage2D> entry: coverages.entrySet()){
                // use GridCoverage2DRIA for adapting GridGeometry between sources
                final GridCoverage2DRIA input = GridCoverage2DRIA.create(
                        entry.getValue(), 
                        destGridGeometry, 
                        org.geotools.resources.coverage.CoverageUtilities.getBackgroundValues(entry.getValue())[0]);
                assert input!=null;
                rias.put(entry.getKey(), input);
                jb.source("image"+k++, input,null,false);
            }
    
            // Now we specify the bounds of the destination image to generate
            // and associate it with the variable `dest`
            // Ignore any ImageLayout that was provided and create one here
            final Dimension defaultTileSize = JAI.getDefaultTileSize();
            SampleModel sm = RasterFactory.createPixelInterleavedSampleModel(
                    DataBuffer.TYPE_DOUBLE, defaultTileSize.width, defaultTileSize.height, 1);
            final GridEnvelope2D gr2d = destGridGeometry.getGridRange2D();
            final WritableRenderedImage img = new TiledImage(gr2d.x,gr2d.y,gr2d.width, gr2d.height,0,0,sm,PlanarImage.createColorModel(sm));    
            jb.dest("dest",img);
            
    
            final long taskSize=gr2d.width* gr2d.height;
            final long step=(long) (taskSize*0.02);
            JiffleProgressListener listener=new NullProgressListener() {
                
                double progress=0;
                
                @Override
                public void update(long ll) {
                    if(ll%step==0){
                        progress+=step/(taskSize*1.0)*100;
                        if(LOGGER.isLoggable(Level.FINE)){
                            LOGGER.fine("Update%: "+Double.toString(progress)+ " at "+new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
                        }
                    }
                    
                }
                
                @Override
                public void start() {
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Start: "+new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
                    }
                    
                }
                
                @Override
                public void finish() {
                    if(LOGGER.isLoggable(Level.FINE)){
                        LOGGER.fine("Finish: "+new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ").format(new Date()));
                    }
                    
                }
            };
            listener.setUpdateInterval(gr2d.width* gr2d.height/20);
            // Finally we run the script and retrieve the resulting image.
            jb.getRuntime().evaluateAll(listener);        
            
            // === create final gridcoverage       
            // create grid geometry
            final GridGeometry2D gg2d = new GridGeometry2D(
                    new GridEnvelope2D(PlanarImage.wrapRenderedImage(img).getBounds()),
                    destGridGeometry.getGridToCRS(),
                    destGridGeometry.getCoordinateReferenceSystem()
            );
            
            // create return coverage reusing origin grid to world 
            final GridCoverage2D retValue = new GridCoverageFactory(hints).create(
                    "RasterAlgebra"+System.nanoTime(), 
                    img,
                    gg2d, 
                    new GridSampleDimension[]{new GridSampleDimension("Binary")},//TODO generalize in the future
                    null,//TODO carry along sources
                    null); // TODO carry along no data

            return retValue;
        } finally{
            // --- clean up
            
            // clean GridCoverage2DRIA map
            for(Map.Entry<String, GridCoverage2DRIA> entry: rias.entrySet()){
                final GridCoverage2DRIA input = entry.getValue();
                if(input!=null){
                    input.dispose();
                }
            }
            
            // jiffle
            jb.clear();
            
            
            // as the destination is in memory we can dispose source
            // clean GridCoverage2D map
            // TODO in the future this would not be doable as I am hoping to make this work in streaming
            for(Map.Entry<String, GridCoverage2D> entry: coverages.entrySet()){
                // use GridCoverage2DRIA for adapting GridGeometry between sources
                ImageUtilities.disposePlanarImageChain(PlanarImage.wrapRenderedImage(entry.getValue().getRenderedImage()));
            }        
        }
    }
}