/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.netcdf;

public enum DataPacking {
    NONE, BYTE, SHORT, INT;

    public static DataPacking getDefault() {
        return NONE;
    }
};
