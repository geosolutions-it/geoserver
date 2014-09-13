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
package org.geoserver.wps.gs.rasteralgebra;

import java.util.List;

import org.geotools.util.Utilities;

/**
 * {@link Enum} that can be used to specify the behavior that we want to specify
 * for when we have to compute the final pixel size.
 * <p>
 * Values are:
 * <ol>
 * <li> {@link ResolutionChoice#MIN} to use the minium value</li>
 * <li> {@link ResolutionChoice#MAX} to use the maximum value</li>
 * <li> {@link ResolutionChoice#AVG} to use the average value</li>
 * <li> {@link ResolutionChoice#FIRST} to use the first value provided</li>
 * </ol>
 * 
 * @author Simone Giannecchini, GeoSolutions SAS
 */
enum ResolutionChoice {
    MIN {
        @Override
        public double compute(List<Double> pixelScales) {
            Utilities.ensureNonNull("pixelScales", pixelScales);
            final int size = pixelScales.size();
            if (size <= 0) {
                throw new IllegalArgumentException("pixelScales list is empty!");
            }
            double appo = pixelScales.get(0);
            for (int i = 1; i < size; i++) {
                appo = Math.min(appo, pixelScales.get(i));
            }
            return appo;
        }
    },
    MAX {
        @Override
        public double compute(List<Double> pixelScales) {
            Utilities.ensureNonNull("pixelScales", pixelScales);
            final int size = pixelScales.size();
            if (size <= 0) {
                throw new IllegalArgumentException("pixelScales list is empty!");
            }
            double appo = pixelScales.get(0);
            for (int i = 1; i < size; i++) {
                appo = Math.max(appo, pixelScales.get(i));
            }
            return appo;
        }
    },
    AVG {
        @Override
        public double compute(List<Double> pixelScales) {
            Utilities.ensureNonNull("pixelScales", pixelScales);
            final int size = pixelScales.size();
            if (size <= 0) {
                throw new IllegalArgumentException("pixelScales list is empty!");
            }
            double appo = pixelScales.get(0);
            for (int i = 1; i < size; i++) {
                appo += pixelScales.get(i);
            }
            return appo / size;
        }
    },
    FIRST {
        @Override
        public double compute(List<Double> pixelScales) {
            Utilities.ensureNonNull("pixelScales", pixelScales);
            if (pixelScales.size() <= 0) {
                throw new IllegalArgumentException("pixelScales list is empty!");
            }
            return pixelScales.get(0);
        }
    };

    public abstract double compute(List<Double> pixelScales);

    public static ResolutionChoice getDefault() {
        return MIN;
    }
}
