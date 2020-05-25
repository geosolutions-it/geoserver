/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical;

import it.geosolutions.jaiext.range.NoDataContainer;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.WPSException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.factory.epsg.CoordinateOperationFactoryUsingWKT;
import org.geotools.referencing.operation.DefaultMathTransformFactory;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.geotools.referencing.operation.transform.ConcatenatedTransform;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.URLs;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.datum.PixelInCell;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.opengis.util.ProgressListener;

/**
 * A Resampling class applying a Vertical Grid Transform to an input coverage representing height
 * data expressed in a source VerticalCRS to produce an output coverage with heigh data expressed in
 * a target VerticalCRS.
 */
public class VerticalResampler {

    private static final double DELTA = 1E-6;

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(VerticalResampler.class);

    /** A cache containing the VerticalGridTransform for a <source,target> key mapping */
    private static final SoftValueHashMap<String, VerticalGridTransform>
            CRS_MAPPING_TO_VERTICAL_GRID_TRANSFORM = new SoftValueHashMap<>();

    private static DefaultMathTransformFactory MT_FACTORY = new DefaultMathTransformFactory();

    static {
        // Let's load the user_projections/epsg_operations.properties and extract the vertical grid
        // definitions

        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File geoserverDataDir = loader.getBaseDirectory();
        final String epsgPropertyFilePath =
                "user_projections"
                        + File.separatorChar
                        + CoordinateOperationFactoryUsingWKT.FILENAME;
        File epsgPropertiesFile = new File(geoserverDataDir, epsgPropertyFilePath);

        if (epsgPropertiesFile.exists()) {
            Properties definitions = new Properties();
            URL url = URLs.fileToUrl(epsgPropertiesFile);
            // Load properties
            try (InputStream in = url.openStream()) {
                definitions.load(in);
            } catch (IOException e) {
                throw new RuntimeException(
                        "Exception occurred while parsing: "
                                + epsgPropertiesFile
                                + e.getLocalizedMessage());
            }
            Set<Map.Entry<Object, Object>> entriesSet = definitions.entrySet();
            for (Map.Entry entry : entriesSet) {
                // Scan the mapping entries and process only the vertical offset ones
                String value = (String) entry.getValue();
                if (value.contains(
                        VerticalGridTransform.Provider.VERTICAL_OFFSET_BY_GRID_INTERPOLATION_KEY)) {
                    String key = (String) entry.getKey();
                    try {
                        MathTransform mt = MT_FACTORY.createFromWKT(value);
                        if (mt instanceof VerticalGridTransform) {
                            CRS_MAPPING_TO_VERTICAL_GRID_TRANSFORM.put(
                                    key, (VerticalGridTransform) mt);
                        }
                    } catch (FactoryException e) {
                        if (LOGGER.isLoggable(Level.SEVERE)) {
                            LOGGER.severe(
                                    "Unable to parse the Vertical Grid Interpolation: "
                                            + value
                                            + "="
                                            + key
                                            + " due to "
                                            + e.getLocalizedMessage());
                        }
                    }
                }
            }
        }
    }

    private ProgressListener progressListener;

    private final GridCoverageFactory gcFactory;

    /** The VerticalGridTransform being used by this resampler */
    private VerticalGridTransform verticalGridTransform;

    /** Source Vertical CRS */
    private CoordinateReferenceSystem sourceVerticalCRS;

    /** Target Vertical CRS */
    private CoordinateReferenceSystem targetVerticalCRS;

    /** VerticalGrid's internal CRS */
    private CoordinateReferenceSystem gridCRS;

    /**
     * VerticalResampler constructor
     *
     * @param sourceVerticalCRS the source VerticalCRS
     * @param targetVerticalCRS the target VerticalCRS
     * @param gcFactory a GridCoverageFactory being used to produce an output GridCoverage
     * @param progressListener
     * @throws FactoryException
     */
    public VerticalResampler(
            CoordinateReferenceSystem sourceVerticalCRS,
            CoordinateReferenceSystem targetVerticalCRS,
            GridCoverageFactory gcFactory,
            ProgressListener progressListener)
            throws FactoryException {
        this.sourceVerticalCRS = sourceVerticalCRS;
        this.targetVerticalCRS = targetVerticalCRS;
        this.progressListener = progressListener;
        this.gcFactory = gcFactory;
        int sourceCode = CRS.lookupEpsgCode(sourceVerticalCRS, false);
        int targetCode = CRS.lookupEpsgCode(targetVerticalCRS, false);
        String sourceToTargetMapping = sourceCode + "," + targetCode;
        // Let's check if we have a Vertical Grid Shift file for this CRSs
        verticalGridTransform = CRS_MAPPING_TO_VERTICAL_GRID_TRANSFORM.get(sourceToTargetMapping);
        if (verticalGridTransform != null) {
            int epsgCode = verticalGridTransform.getVerticalGridShift().getCRSCode();
            if (epsgCode != Integer.MIN_VALUE) {
                gridCRS = CRS.decode("EPSG:" + epsgCode);
            }
        } else {
            throw new WPSException(
                    "No Vertical Transformation has been found from "
                            + sourceVerticalCRS
                            + " to "
                            + targetVerticalCRS);
        }
    }

    /**
     * Resample the provided gridCoverage by applying the underlying VerticalGridTransform, to
     * adjust the height values
     *
     * @param gridCoverage
     * @return
     * @throws FactoryException
     * @throws TransformException
     */
    public GridCoverage2D resample(GridCoverage2D gridCoverage)
            throws FactoryException, TransformException {
        CoordinateReferenceSystem inputCRS = gridCoverage.getCoordinateReferenceSystem();
        Envelope dataEnvelope = new ReferencedEnvelope(gridCoverage.getEnvelope());
        VerticalGridShift gridShift = verticalGridTransform.getVerticalGridShift();
        MathTransform sourceToGridCrsTransform = null;

        if (!CRS.equalsIgnoreMetadata(inputCRS, gridCRS)) {
            // Input data is not in the same CRS as the vertical Grid
            sourceToGridCrsTransform = CRS.findMathTransform(inputCRS, gridCRS);
            dataEnvelope = JTS.transform(dataEnvelope, sourceToGridCrsTransform);
        }

        ReferencedEnvelope gridEnvelope = new ReferencedEnvelope(gridShift.getValidArea());
        if (!gridEnvelope.intersects(dataEnvelope)) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(
                        "The computed GridCoverage doesn't intersect the the valid area of the available grid.\""
                                + " Data Envelope: "
                                + dataEnvelope
                                + " Vertical Grid File Envelope: "
                                + gridEnvelope
                                + ".\n Returning the coverage without vertical interpolation being applied");
            }
            return gridCoverage;
        }
        GridGeometry2D gridGeometry = gridCoverage.getGridGeometry();
        MathTransform gridToCrs = gridGeometry.getGridToCRS(PixelInCell.CELL_CENTER);

        // Prepare concatenated transforms needed to go from input coverage pixels to the vertical
        // grid positions
        MathTransform pixelToVerticalGridTransform =
                sourceToGridCrsTransform != null
                        ? ConcatenatedTransform.create(gridToCrs, sourceToGridCrsTransform)
                        : gridToCrs;
        double[] resolution = gridShift.getResolution();

        // Apply an half pixel translate to center on the Vertical Grid pixels
        AffineTransform halfPixelTranslate =
                AffineTransform.getTranslateInstance(-resolution[0] / 2d, resolution[1] / 2d);
        pixelToVerticalGridTransform =
                ConcatenatedTransform.create(
                        pixelToVerticalGridTransform, new AffineTransform2D(halfPixelTranslate));

        PlanarImage image = null;
        WritableRaster raster = null;
        RandomIter iter = null;
        NoDataContainer noDataContainer = null;
        try {
            double srcNoDataValue = Double.NaN;
            image = PlanarImage.wrapRenderedImage(gridCoverage.getRenderedImage());
            Object property = image.getProperty(NoDataContainer.GC_NODATA);
            if (property != null) {
                if (property instanceof NoDataContainer) {
                    noDataContainer = ((NoDataContainer) property);
                    srcNoDataValue = noDataContainer.getAsSingleValue();
                }
            }

            // This iterator goes through tiled images if any
            iter = RandomIterFactory.create(image, image.getBounds());
            final int width = image.getWidth();
            final int height = image.getHeight();
            int minX = image.getMinX();
            int minY = image.getMinY();

            SampleModel targetSampleModel =
                    RasterFactory.createComponentSampleModel(
                            image.getSampleModel(),
                            image.getSampleModel().getDataType(),
                            width,
                            height,
                            1);
            raster = RasterFactory.createWritableRaster(targetSampleModel, new Point());
            // TODO Switch to a Tile based approach here

            double[] srcPoints = new double[3];
            double[] destPoints = new double[3];
            final int maxY = image.getMaxY();
            for (int j = minY; j < maxY; j++) {
                for (int i = minX; i < image.getMaxX(); i++) {
                    srcPoints[0] = i;
                    srcPoints[1] = j;
                    srcPoints[2] = iter.getSampleDouble(i, j, 0);
                    if (Double.isNaN(srcPoints[2])
                            || Math.abs(srcNoDataValue - srcPoints[2]) < DELTA) {
                        destPoints[2] = srcPoints[2];
                    } else {
                        // Transform the pixel coordinate to the coordinate in the vertical grid crs
                        pixelToVerticalGridTransform.transform(srcPoints, 0, srcPoints, 0, 1);
                        // Transform the vertical value of the current position
                        verticalGridTransform.transform(srcPoints, 0, destPoints, 0, 1);
                        if (Double.isNaN(destPoints[2])) {
                            destPoints[2] = srcNoDataValue;
                        }
                    }

                    raster.setSample(i - minX, j - minY, 0, destPoints[2]);
                }
            }
        } finally {
            if (image != null) {
                ImageUtilities.disposeImage(image);
            }
            if (iter != null) {
                iter.done();
            }
        }
        if (raster == null) {
            throw new WPSException("Unable to create a raster with the updated vertical values.");
        }
        return gcFactory.create(
                gridCoverage.getName(),
                raster,
                gridCoverage.getEnvelope(),
                gridCoverage.getSampleDimensions());
    }
}
