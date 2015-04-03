/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.geoserver.wcs.responses.NetCDFCoordinateReferenceSystem.NetCDFCoordinate;
import org.geoserver.wcs.responses.NetCDFDimensionManager.DimensionValuesArray;
import org.geoserver.wcs2_0.response.DimensionBean;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.CRS;
import org.geotools.referencing.CRS.AxisOrder;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import ucar.ma2.ArrayFloat;
import ucar.ma2.DataType;
import ucar.ma2.Index;
import ucar.ma2.InvalidRangeException;
import ucar.nc2.Attribute;
import ucar.nc2.Dimension;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;

/**
 * An inner class delegated to add Coordinates to the output NetCDF file,
 * as well as setting additional attributes and variables needed to properly
 * represent the related CoordinateReferenceSystem.
 * 
 * Note that NetCDF files write is made in 2 steps:
 * 1) the data initialization (define mode)
 * 2) the data write
 * 
 * Therefore, the NetCDFCoordinates writer needs to be initialized first, through the 
 * {@link #initializeCoordinatesDimensions(Map)}
 * 
 * Once all other elements of the NetCDF file have been initialized, the Coordinates
 * need to be written through the #
 * 
 * @author Daniele Romagnoli, GeoSolutions
 *
 */
class NetCDFCoordinatesWriter {

    /** the NetCDF CoordinateReferenceSystem holder */
    private NetCDFCoordinateReferenceSystem supportedCrs;

    /** the NetCDF File writer */
    private NetcdfFileWriter writer;

    /**
     * A sample granule used to extract properties such as CoordinateReferenceSystem, 
     * Grid2World transformation
     */
    private GridCoverage2D sampleGranule;

    /** A map to assign a Dimension manager to each coordinate */
    private Map<String, NetCDFDimensionManager> coordinatesDimensions = new LinkedHashMap<String, NetCDFDimensionManager>();

    /** The underlying CoordinateReferenceSystem */
    private CoordinateReferenceSystem crs;
    
    /** The Grid2World transformation, used to setup geoTransformation */
    private MathTransform transform;

    public NetCDFCoordinatesWriter(NetcdfFileWriter writer, GridCoverage2D sampleGranule) {
        this.writer = writer;
        this.sampleGranule = sampleGranule;
        GridGeometry gridGeometry = sampleGranule.getGridGeometry();
        transform = gridGeometry.getGridToCRS();
        crs = sampleGranule.getCoordinateReferenceSystem();
        supportedCrs = NetCDFCoordinateReferenceSystem.parseCRS(crs);
    }

    /**
     * Setup lat,lon dimension (or y,x)  and related coordinates variable
     */
    public void initializeCoordinatesDimensions(Map<String, NetCDFDimensionManager> dimensionMapping) {
        final RenderedImage image = sampleGranule.getRenderedImage();
        final Envelope envelope = sampleGranule.getEnvelope2D();

        AxisOrder axisOrder = CRS.getAxisOrder(crs);

        final int height = image.getHeight();
        final int width = image.getWidth();

        final AffineTransform at = (AffineTransform) transform;

        NetCDFCoordinate[] axisCoordinates = supportedCrs.getCoordinates();

        // Setup resolutions and bbox extrema to populate regularly gridded coordinate data
        //TODO: investigate whether we need to do some Y axis flipping
        double xmin = (axisOrder == AxisOrder.NORTH_EAST) ? envelope.getMinimum(1) : envelope.getMinimum(0);
        double ymin = (axisOrder == AxisOrder.NORTH_EAST) ? envelope.getMinimum(0) : envelope.getMinimum(1);
        final double periodY = ((axisOrder == AxisOrder.NORTH_EAST) ? XAffineTransform.getScaleX0(at) : XAffineTransform.getScaleY0(at));
        final double periodX = (axisOrder == AxisOrder.NORTH_EAST) ? XAffineTransform.getScaleY0(at) : XAffineTransform.getScaleX0(at);

        // NetCDF coordinates are relative to center. Envelopes are relative to corners: apply an half pixel shift to go back to center
        xmin += (periodX / 2d);
        ymin += (periodY / 2d);

        // -----------------------------------------
        // First coordinate (latitude/northing, ...)
        // -----------------------------------------
        addCoordinateVariable(axisCoordinates[0], height, ymin, periodY);

        // ------------------------------------------
        // Second coordinate (longitude/easting, ...)
        // ------------------------------------------
        addCoordinateVariable(axisCoordinates[1], width, xmin, periodX);
        dimensionMapping.putAll(coordinatesDimensions);

    }

    /** 
     * Add a coordinate variable to the dataset, along with the related dimension.
     * Finally, add the created dimension to the coordinates map
     * */
    private void addCoordinateVariable(NetCDFCoordinate netCDFCoordinate, int size, double min,
            double period) {
        String dimensionName = netCDFCoordinate.getDimensionName();
        String standardName = netCDFCoordinate.getStandardName();
        final Dimension dimension = writer.addDimension(null, dimensionName, size);
        final ArrayFloat dimensionData = new ArrayFloat(new int[] { size });
        final Index index = dimensionData.getIndex();
        final Variable coordinateVariable = writer.addVariable(null, netCDFCoordinate.getShortName(), DataType.FLOAT, dimensionName);
        writer.addVariableAttribute(coordinateVariable, new Attribute(NetCDFUtilities.LONG_NAME, netCDFCoordinate.getLongName()));
        writer.addVariableAttribute(coordinateVariable, new Attribute(NetCDFUtilities.UNITS, netCDFCoordinate.getUnits()));
        if (standardName != null && !standardName.isEmpty()) {
            writer.addVariableAttribute(coordinateVariable, new Attribute(NetCDFUtilities.STANDARD_NAME, standardName));
        }

        for (int pos = 0; pos < size; pos++) {
            dimensionData.setFloat(index.set(pos),
            // new Float(ymax - (new Float(yPos).floatValue() * periodY)).floatValue());
                    new Float(min + (new Float(pos).floatValue() * period)).floatValue());
        }

        final NetCDFDimensionManager dimensionManager = new NetCDFDimensionManager(dimensionName);
        dimensionManager.setNetCDFDimension(dimension);
        dimensionManager.setDimensionValues(new DimensionValuesArray(dimensionData));
        coordinatesDimensions.put(dimensionName, dimensionManager);
    }

    /**
     * Set the coordinate values for all the dimensions
     * 
     * @param writer
     * @throws IOException
     * @throws InvalidRangeException
     */
    void setCoordinateVariable(NetCDFDimensionManager manager) throws IOException,
            InvalidRangeException {
        Dimension dimension = manager.getNetCDFDimension();
        if (dimension == null) {
            throw new IllegalArgumentException("No Dimension found for this manager: "
                    + manager.getName());
        }

        // Getting coordinate variable for that dimension
        final String dimensionName = dimension.getShortName();
        Variable var = writer.findVariable(dimensionName);
        if (var == null) {
            throw new IllegalArgumentException("Unable to find the specified coordinate variable: "
                    + dimensionName);
        }
        // Writing coordinate variable values
        writer.write(var, manager.getDimensionData(false, supportedCrs.getCoordinates()));

        // handle ranges
        DimensionBean coverageDimension = manager.getCoverageDimension();
        if (coverageDimension != null) { // 2D coords (lat,lon / x,y) may be null
            boolean isRange = coverageDimension.isRange();
            if (isRange) {
                var = writer.findVariable(dimensionName + NetCDFUtilities.BOUNDS_SUFFIX);
                writer.write(var, manager.getDimensionData(true, null));
            }
        }
    }

    /**
     * Add gridMapping variable for projected datasets.
     * 
     * @param var the {@link Variable} where the mapping attribute needs to be appended
     */
    void initializeGridMapping(Variable var) {
        String gridMapping = supportedCrs.getGridMapping();
        if (gridMapping != null && !gridMapping.isEmpty()) {
            if (var != null) {
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.GRID_MAPPING,
                        gridMapping));
            }
            writer.addVariable(null, gridMapping, DataType.CHAR, (String) null);
        }
        addProjectionInformation(supportedCrs, writer, crs, transform);
    }
    
    /**
     * Add GeoReferencing information to the writer, starting from the CoordinateReferenceSystem and the MathTransform
     * 
     * @param writer
     * @param crs
     * @param transform
     */
    public void addProjectionInformation(NetCDFCoordinateReferenceSystem supportedCrs,
            NetcdfFileWriter writer, CoordinateReferenceSystem crs, MathTransform transform) {
        String gridMapping = supportedCrs.getGridMapping();

        if (!gridMapping.isEmpty()) {
            NetCDFProjectionParametersManager params = supportedCrs
                    .getNetCDFProjectionParametersManager();
            Variable var = writer.findVariable(gridMapping);
            params.setProjectionParams(writer, crs, var);
            addMappingAttributes(writer, crs, transform, var, gridMapping);
        } else {
            addGlobalAttributes(writer, crs, transform);
        }
    }

    /**
     * Add GeoReferencing global attributes (GDAL's spatial_ref and GeoTransform).
     * They will be used for dataset with unsupported NetCDF CF projection.

     * @param writer
     * @param crs
     * @param transform
     */
    private void addGlobalAttributes(NetcdfFileWriter writer, CoordinateReferenceSystem crs,
            MathTransform transform) {
        writer.addGroupAttribute(null, getSpatialRefAttribute(crs));
        writer.addGroupAttribute(null, getGeoTransformAttribute(transform));
    }

    /**
     * 
     * @param writer
     * @param crs
     * @param transform
     * @param var
     * @param gridMapping
     */
    private void addMappingAttributes(NetcdfFileWriter writer, CoordinateReferenceSystem crs,
            MathTransform transform, Variable var, String gridMapping) {

        writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.GRID_MAPPING_NAME,
                gridMapping));

        // Adding GDAL Attributes spatial_ref and GeoTransform
        writer.addVariableAttribute(var, getSpatialRefAttribute(crs));
        writer.addVariableAttribute(var, getGeoTransformAttribute(transform));
    }

    /**
     * Setup a {@link NetCDFUtilities#SPATIAL_REF} attribute on top of the CoordinateReferenceSystem
     * 
     * @param crs the {@link CoordinateReferenceSystem} instance
     * @return the {@link Attribute} containing the spatial_ref  attribute
     */
    private Attribute getSpatialRefAttribute(CoordinateReferenceSystem crs) {
        String wkt = crs.toWKT().replace("\r\n", "").replace("  ", " ").replace("  ", " ");
        return new Attribute(NetCDFUtilities.SPATIAL_REF, wkt);
    }

    /**
     * Setup a {@link NetCDFUtilities#GEO_TRANSFORM} attribute on top of the MathTransform
     * 
     * @param MathTransform the grid2world geoTransformation
     * @return the {@link Attribute} containing the GeotTransform attribute
     */
    private Attribute getGeoTransformAttribute(MathTransform transform) {
        AffineTransform at = (AffineTransform) transform;
        String geoTransform = Double.toString(at.getTranslateX()) + " "
                + Double.toString(at.getScaleX()) + " " + Double.toString(at.getShearX()) + " "
                + Double.toString(at.getTranslateY()) + " " + Double.toString(at.getShearY()) + " "
                + Double.toString(at.getScaleY());
        return new Attribute(NetCDFUtilities.GEO_TRANSFORM, geoTransform);

    }

   
}
