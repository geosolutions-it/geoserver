/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning.views;

/**
 * Utility class for testing purposes. It allows to force a specific timestamp in the lastUpdate
 * filter of the user preferences invokation.
 */
public class TestContext {
    private static String UPDATE_TIME;

    public static void setUpdateTime(String value) {
        UPDATE_TIME = value;
    }

    public static String getUpdateTime() {
        return UPDATE_TIME;
    }

    public static void clear() {
        UPDATE_TIME = null;
    }
}
