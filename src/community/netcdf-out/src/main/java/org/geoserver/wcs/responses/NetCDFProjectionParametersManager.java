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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.referencing.operation.projection.MapProjection;
import org.geotools.util.logging.Logging;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.ProjectedCRS;
import org.opengis.referencing.operation.Projection;

import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFileWriter;
import ucar.nc2.Variable;
import ucar.nc2.constants.CF;

/** 
 * Class used to properly setup NetCDF CF Projection parameters. Given a known Projection, 
 * it will take care of remap the Projection's parameters to NetCDF CF GridMapping parameters.
 * 
 * @see <a href="http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings">NetCDF CF, Appendix
 *      F: Grid Mappings</a>
 */
class NetCDFProjectionParametersManager {

    public static final Logger LOGGER = Logging.getLogger(NetCDFProjectionParametersManager.class);

    public static final String STANDARD_PARALLEL_1 = MapProjection.AbstractProvider.STANDARD_PARALLEL_1
            .getName().getCode();

    public static final String STANDARD_PARALLEL_2 = MapProjection.AbstractProvider.STANDARD_PARALLEL_2
            .getName().getCode();

    public static final String CENTRAL_MERIDIAN = MapProjection.AbstractProvider.CENTRAL_MERIDIAN
            .getName().getCode();

    public static final String LATITUDE_OF_ORIGIN = MapProjection.AbstractProvider.LATITUDE_OF_ORIGIN
            .getName().getCode();

    public static final String SCALE_FACTOR = MapProjection.AbstractProvider.SCALE_FACTOR.getName()
            .getCode();

    public static final String FALSE_EASTING = MapProjection.AbstractProvider.FALSE_EASTING
            .getName().getCode();

    public static final String FALSE_NORTHING = MapProjection.AbstractProvider.FALSE_NORTHING
            .getName().getCode();

    public NetCDFProjectionParametersManager(Map<String, String> parametersMapping) {
        this.referencingToNetCDFParameters = parametersMapping;
    }

    /** 
     * Setup proper projection information to the output NetCDF 
     */
    public void setProjectionParams(NetcdfFileWriter writer, CoordinateReferenceSystem crs,
            Variable var) {
        if (!(crs instanceof ProjectedCRS)) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("The provided CRS is not a projected CRS\n"
                        + "No projection information needs to be added");
            }
            return;
        }
        ProjectedCRS projectedCRS = (ProjectedCRS) crs;
        Projection conversionFromBase = projectedCRS.getConversionFromBase();
        if (conversionFromBase != null) {

            // getting the list of parameters needed for the NetCDF mapping
            Set<String> keySet = referencingToNetCDFParameters.keySet();

            // getting the list of parameters from the Projection 
            ParameterValueGroup values = conversionFromBase.getParameterValues();
            List<GeneralParameterValue> valuesList = values.values();

            Map<String, List<Double>> parameterValues = new HashMap<String, List<Double>>();

            // Loop over the available conversion parameters
            for (GeneralParameterValue param : valuesList) {
                String code = param.getDescriptor().getName().getCode();

                // Check if one of the parameters is needed by NetCDF
                if (keySet.contains(code)) {

                    // Get the parameter value
                    Double value = ((ParameterValue) param).doubleValue();
                    String mappedKey = referencingToNetCDFParameters.get(code);

                    // Make sure to proper deal with Number and Arrays
                    if (parameterValues.containsKey(mappedKey)) {
                        List<Double> paramValues = parameterValues.get(mappedKey);
                        paramValues.add(value);
                    } else {
                        List<Double> paramValues = new ArrayList<Double>(1);
                        paramValues.add(value);
                        parameterValues.put(mappedKey, paramValues);
                    }
                }
            }

            // Setup projections attributes
            Set<String> paramKeys = parameterValues.keySet();
            for (String key: paramKeys) {
                List<Double> val = parameterValues.get(key);
                if (val.size() == 1) {
                    writer.addVariableAttribute(var, new Attribute(key, val.get(0)));
                } else {
                    writer.addVariableAttribute(var, new Attribute(key, val));
                }
            }
        }
    }

    private Map<String, String> referencingToNetCDFParameters;

    /**
     * Currently supported NetCDF projections. TODO: Add more. Check the CF Document
     * 
     * @see <a href="http://cfconventions.org/Data/cf-conventions/cf-conventions-1.6/build/cf-conventions.html#appendix-grid-mappings">NetCDF CF,
     *      Appendix F: Grid Mappings</a>
     */
    public final static NetCDFProjectionParametersManager TRANSVERSE_MERCATOR_PARAMS;
    public final static NetCDFProjectionParametersManager LAMBERT_CONFORMAL_CONIC_1SP_PARAMS;
    public final static NetCDFProjectionParametersManager LAMBERT_CONFORMAL_CONIC_2SP_PARAMS;

    static {
        Map<String, String> tm_mapping = new HashMap<String, String>();
        tm_mapping.put(SCALE_FACTOR, CF.SCALE_FACTOR_AT_CENTRAL_MERIDIAN);
        tm_mapping.put(CENTRAL_MERIDIAN, CF.LONGITUDE_OF_CENTRAL_MERIDIAN);
        tm_mapping.put(LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        tm_mapping.put(FALSE_EASTING, CF.FALSE_EASTING);
        tm_mapping.put(FALSE_NORTHING, CF.FALSE_NORTHING);
        TRANSVERSE_MERCATOR_PARAMS = new NetCDFProjectionParametersManager(tm_mapping);

        Map<String, String> lcc_mapping = new HashMap<String, String>();

        lcc_mapping.put(CENTRAL_MERIDIAN, CF.LONGITUDE_OF_CENTRAL_MERIDIAN);
        lcc_mapping.put(LATITUDE_OF_ORIGIN, CF.LATITUDE_OF_PROJECTION_ORIGIN);
        lcc_mapping.put(FALSE_EASTING, CF.FALSE_EASTING);
        lcc_mapping.put(FALSE_NORTHING, CF.FALSE_NORTHING);

        Map<String, String> lcc_1sp_mapping = new HashMap<String, String>();
        lcc_1sp_mapping.putAll(lcc_mapping);
        lcc_1sp_mapping.put(STANDARD_PARALLEL_1, CF.STANDARD_PARALLEL);
        LAMBERT_CONFORMAL_CONIC_1SP_PARAMS = new NetCDFProjectionParametersManager(lcc_1sp_mapping);

        Map<String, String> lcc_2sp_mapping = new HashMap<String, String>();
        lcc_2sp_mapping.putAll(lcc_mapping);
        lcc_2sp_mapping.put(STANDARD_PARALLEL_1, CF.STANDARD_PARALLEL);
        lcc_2sp_mapping.put(STANDARD_PARALLEL_2, CF.STANDARD_PARALLEL);
        LAMBERT_CONFORMAL_CONIC_2SP_PARAMS = new NetCDFProjectionParametersManager(lcc_2sp_mapping);

    }
}