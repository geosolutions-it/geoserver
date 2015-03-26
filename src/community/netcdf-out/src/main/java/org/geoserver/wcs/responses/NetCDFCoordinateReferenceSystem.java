/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2015, Open Source Geospatial Foundation (OSGeo)
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
package org.geoserver.wcs.responses;

import java.awt.geom.AffineTransform;

import org.geotools.imageio.netcdf.utilities.NetCDFUtilities;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.projection.LambertConformal1SP;
import org.geotools.referencing.operation.projection.LambertConformal2SP;
import org.geotools.referencing.operation.projection.TransverseMercator;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.Projection;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

public enum NetCDFCoordinateReferenceSystem {

    WGS84 {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.LATLON_COORDS;
        }

        @Override
        public NetCDFProjectionParametersManager getNetCDFProjectionParametersManager() {
            return null;
        };

    },
    NONE {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public NetCDFProjectionParametersManager getNetCDFProjectionParametersManager() {
            return null;
        };
    },
    LAMBERT_CONFORMAL_CONIC_1SP {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjectionParametersManager getNetCDFProjectionParametersManager() {
            return NetCDFProjectionParametersManager.LAMBERT_CONFORMAL_CONIC_1SP_PARAMS;
        };

        @Override
        public String getGridMapping() {
            return CF.LAMBERT_CONFORMAL_CONIC+"_1SP";
        }
    },
    LAMBERT_CONFORMAL_CONIC_2SP {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjectionParametersManager getNetCDFProjectionParametersManager() {
            return NetCDFProjectionParametersManager.LAMBERT_CONFORMAL_CONIC_2SP_PARAMS;
        };

        @Override
        public String getGridMapping() {
            return CF.LAMBERT_CONFORMAL_CONIC+"_2SP";
        }
    },

    TRANSVERSE_MERCATOR {
        @Override
        public NetCDFCoordinate[] getCoordinates() {
            return NetCDFCoordinate.YX_COORDS;
        }

        @Override
        public NetCDFProjectionParametersManager getNetCDFProjectionParametersManager() {
            return NetCDFProjectionParametersManager.TRANSVERSE_MERCATOR_PARAMS;
        };

        @Override
        public String getGridMapping() {
            return CF.TRANSVERSE_MERCATOR;
        }
    }

    /*
     * , ALBERS_EQUAL_AREA, AZIMUTHAL_EQUIDISTANT, LAMBERT_AZIMUTHAL_EQUAL_AREA, LAMBERT_CONFORMAL, LAMBERT_CYLINDRICAL_EQUAL_AREA, MERCATOR,
     * ORTOGRAPHIC, POLAR_STEREOGRAPHIC, ROTATED_POLE, STEREOGRAPHIC,
     */;

    public static NetCDFCoordinateReferenceSystem parseCRS(CoordinateReferenceSystem crs) {
        if (crs instanceof DefaultGeographicCRS) {
            return WGS84;
        } else if (crs instanceof ProjectedCRS) {
            ProjectedCRS projectedCRS = (ProjectedCRS) crs;
            Projection projection = projectedCRS.getConversionFromBase();
            MathTransform transform = projection.getMathTransform();
            if (transform instanceof TransverseMercator) {
                return TRANSVERSE_MERCATOR;
            } else if (transform instanceof LambertConformal1SP) {
                return LAMBERT_CONFORMAL_CONIC_1SP;
            } else if (transform instanceof LambertConformal2SP) {
                return LAMBERT_CONFORMAL_CONIC_2SP;
            }
        }
        return NONE;
    }

    public abstract NetCDFCoordinate[] getCoordinates();

    public abstract NetCDFProjectionParametersManager getNetCDFProjectionParametersManager();

    public String getGridMapping() {
        return "";
    }

    /**
     * Add GeoReferencing information to the writer, starting from the CoordinateReferenceSystem 
     * and the MathTransform
     * 
     * @param writer
     * @param crs
     * @param transform
     */
    public void addProjectionInformation(NetcdfFileWriter writer, CoordinateReferenceSystem crs,
            MathTransform transform) {
        String gridMapping = getGridMapping();
        if (!gridMapping.isEmpty()) {
            NetCDFProjectionParametersManager params = getNetCDFProjectionParametersManager();
            Variable var = writer.findVariable(gridMapping);
            params.setProjectionParams(writer, crs, var);
            addCommonAttributes(writer, crs, transform, var, gridMapping);
        } else {
            
        }
    }

    /**
     * 
     * @param writer
     * @param crs
     * @param transform
     * @param var
     * @param gridMapping
     */
    private void addCommonAttributes(NetcdfFileWriter writer, CoordinateReferenceSystem crs,
            MathTransform transform, Variable var, String gridMapping) {

        writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.GRID_MAPPING_NAME,
                gridMapping));

        // Adding GDAL Attributes spatial_ref and GeoTransform
        String wkt = crs.toWKT().replace("\r\n", "").replace("  ", " ").replace("  ", " ");
        writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.SPATIAL_REF, wkt));
        AffineTransform at = (AffineTransform) transform;
        String geoTransform = Double.toString(at.getTranslateX()) + " "
                + Double.toString(at.getScaleX()) + " " + Double.toString(at.getShearX()) + " "
                + Double.toString(at.getTranslateY()) + " " + Double.toString(at.getShearY()) + " "
                + Double.toString(at.getScaleY());
        writer.addVariableAttribute(var, new Attribute(NetCDFUtilities.GEO_TRANSFORM, geoTransform));
    }

    /** 
     * Contains basic information related to a NetCDF Coordinate such as:
     *  - long name
     *  - short name
     *  - standard name
     *  - dimension name
     *  - unit
     * */
    public static class NetCDFCoordinate {

        private final static NetCDFCoordinate LAT_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.LAT, NetCDFUtilities.LATITUDE, NetCDFUtilities.LATITUDE,
                NetCDFUtilities.LAT, NetCDFUtilities.LAT_UNITS);

        private final static NetCDFCoordinate LON_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.LON, NetCDFUtilities.LONGITUDE, NetCDFUtilities.LONGITUDE,
                NetCDFUtilities.LON, NetCDFUtilities.LON_UNITS);

        private final static NetCDFCoordinate X_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.X, NetCDFUtilities.X_COORD_PROJ, NetCDFUtilities.X_PROJ_COORD,
                NetCDFUtilities.X, NetCDFUtilities.M);

        private final static NetCDFCoordinate Y_COORDINATE = new NetCDFCoordinate(
                NetCDFUtilities.Y, NetCDFUtilities.Y_COORD_PROJ, NetCDFUtilities.Y_PROJ_COORD,
                NetCDFUtilities.Y, NetCDFUtilities.M);

        private final static NetCDFCoordinate[] LATLON_COORDS = new NetCDFCoordinate[] {
                LAT_COORDINATE, LON_COORDINATE };

        private final static NetCDFCoordinate[] YX_COORDS = new NetCDFCoordinate[] { Y_COORDINATE,
                X_COORDINATE };

        private String shortName;

        private String dimensionName;

        private String longName;

        private String units;

        private String standardName;

        public String getShortName() {
            return shortName;
        }

        public void setShortName(String shortName) {
            this.shortName = shortName;
        }

        public String getDimensionName() {
            return dimensionName;
        }

        public void setDimensionName(String dimensionName) {
            this.dimensionName = dimensionName;
        }

        public String getLongName() {
            return longName;
        }

        public void setName(String longName) {
            this.longName = longName;
        }

        public String getStandardName() {
            return standardName;
        }

        public void setStandardName(String standardName) {
            this.standardName = standardName;
        }

        public String getUnits() {
            return units;
        }

        public void setUnits(String units) {
            this.units = units;
        }

        @Override
        public String toString() {
            return "NetCDFCoordinate [shortName=" + shortName + ", dimensionName=" + dimensionName
                    + ", longName=" + longName + ", units=" + units + ", standardName=" + standardName
                    + "]";
        }

        public NetCDFCoordinate(String shortName, String longName, String standardName,
                String dimensionName, String units) {
            this.shortName = shortName;
            this.longName = longName;
            this.standardName = standardName;
            this.dimensionName = dimensionName;
            this.units = units;
        }

    }

};
