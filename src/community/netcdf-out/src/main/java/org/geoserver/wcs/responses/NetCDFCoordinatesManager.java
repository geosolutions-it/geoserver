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

class NetCDFCoordinatesManager {

    private NetCDFCoordinateReferenceSystem supportedCrs;
    private NetcdfFileWriter writer;
    private GridCoverage2D sampleGranule;
    private Map<String, NetCDFDimensionManager> coordinatesDimensions = new LinkedHashMap<String, NetCDFDimensionManager>();
    private CoordinateReferenceSystem crs;
    private MathTransform transform;

    public NetCDFCoordinatesManager(NetcdfFileWriter writer, GridCoverage2D sampleGranule) {
        this.writer = writer;
        this.sampleGranule = sampleGranule;
        setupCoordinates();
    }

    /**
     * Setup lat,lon dimension (or y,x)  and related coordinates variable
     */
    private void setupCoordinates() {
        final RenderedImage image = sampleGranule.getRenderedImage();
        final Envelope envelope = sampleGranule.getEnvelope2D();

        GridGeometry gridGeometry = sampleGranule.getGridGeometry();
        transform = gridGeometry.getGridToCRS();
        crs = sampleGranule.getCoordinateReferenceSystem();

        supportedCrs = NetCDFCoordinateReferenceSystem.parseCRS(crs);

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
    }

    /** 
     * Add a coordinate variable to the dataset, along with the related dimension
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

    public Map<String, NetCDFDimensionManager> getCoordinatesDimensions() {
        return coordinatesDimensions;
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
    public void initializeGridMapping(Variable var) {
        String gridMapping = supportedCrs.getGridMapping();
        if (gridMapping != null && !gridMapping.isEmpty()) {
            if (var != null) {
                writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.GRID_MAPPING,
                        gridMapping));
            }
            writer.addVariable(null, gridMapping, DataType.CHAR, (String) null);
        }
        supportedCrs.addProjectionInformation(writer, crs, transform);
    }
}
