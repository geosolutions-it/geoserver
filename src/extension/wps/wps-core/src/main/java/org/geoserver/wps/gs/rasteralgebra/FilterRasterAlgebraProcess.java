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

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.IOException;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;

import org.geoserver.catalog.Catalog;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geotools.coverage.GridSampleDimension;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.jaitools.imageutils.ImageLayout2;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A process that returns a coverage fully (something which is un-necessarily
 * hard in WCS)
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 * @author Andrea Aime, GeoSolutions SAS
 */
@DescribeProcess(title = "FilterRasterAlgebra", description = "Compute the operations specified by the filter provided on the coverages mentioned in it")
public class FilterRasterAlgebraProcess implements GeoServerProcess {

private Catalog catalog;

public FilterRasterAlgebraProcess(Catalog catalog) {
    this.catalog = catalog;
}

/**
 * @param classes representing the domain of the classes (Mandatory, not empty)
 * @param rasterT0 that is the reference Image (Mandatory)
 * @param rasterT1 rasterT1 that is the update situation (Mandatory)
 * @param roi that identifies the optional ROI (so that could be null)
 * @return
 */
@DescribeResult(name = "FilterRasterAlgebra", description = "FilterRasterAlgebra", type = GridCoverage2D.class)
public GridCoverage2D execute(
        @DescribeParameter(name = "expression", description = "Filter to use on the raster data", min = 1) String rasterAlgebra,
        @DescribeParameter(name = "ROI", min = 0, description = "Region Of Interest") Geometry roi,
        @DescribeParameter(name = "ROICRS", min = 0, description = "Region Of Interest CRS") CoordinateReferenceSystem roiCRS,
        @DescribeParameter(name = "ResolutionChoice", min = 0, description = "How to choose the final resolution") ResolutionChoice resolutionChoice)
        throws IOException {

    // === filter or expression
    Object ra = null;
    try {
        ra = ECQL.toFilter(rasterAlgebra);
    } catch (Exception e) {
        try {
            ra = ECQL.toExpression(rasterAlgebra);
        } catch (Exception e1) {
            throw new WPSException("Unable to parse input expression", e1);
        }
    }

    // === instantiate collector

    // hints for tiling
    final Hints hints = GeoTools.getDefaultHints().clone();
    final ImageLayout2 layout = new ImageLayout2();
    layout.setTileWidth(JAI.getDefaultTileSize().width);
    layout.setTileHeight(JAI.getDefaultTileSize().height);
    hints.add(new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout));

    // collect input coverages
    final CoverageCollector collector = new CoverageCollector(catalog,
            resolutionChoice != null ? resolutionChoice
                    : ResolutionChoice.getDefault(), roi, roiCRS, hints);
    if (ra instanceof Expression) {
        ((Expression) ra).accept(collector, null);
    } else if (ra instanceof Filter) {
        ((Filter) ra).accept(collector, null);
    }

    // instantiate processor
    final CoverageProcessor processor = new CoverageProcessor(
            collector.getCoverages(), collector.getGridGeometry(), hints);
    Object result_ = null;
    if (ra instanceof Expression) {
        result_ = ((Expression) ra).accept(processor, null);
    } else if (ra instanceof Filter) {
        result_ = ((Filter) ra).accept(processor, null);
    }
    if (result_ instanceof RenderedImage) {

        // === create final gridcoverage

        final RenderedImage raster = (RenderedImage) result_;

        // create grid geometry
        final GridGeometry2D gg2d = new GridGeometry2D(new GridEnvelope2D(
                PlanarImage.wrapRenderedImage(raster).getBounds()), collector
                .getGridGeometry().getGridToCRS(), collector.getGridGeometry()
                .getCoordinateReferenceSystem());

        // create return coverage reusing origin grid to world
        return new GridCoverageFactory(hints)
                .create("RasterAlgebra" + System.nanoTime(), raster, gg2d,
                        new GridSampleDimension[] { new GridSampleDimension(
                                "Binary") },// TODO generalize in the future
                        null,// TODO carry along sources
                        null); // TODO carry along no data
    }

    // something bad happened
    throw new WPSException(
            "Unable to create a valid response for this request.");
}
}
