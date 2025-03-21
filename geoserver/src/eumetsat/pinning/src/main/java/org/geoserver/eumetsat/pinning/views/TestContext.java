package org.geoserver.eumetsat.pinning.views;

// Remove it after deployment. this is needed for testing
public class TestContext {
    private static String UPDATE_TIME;

    public static void setUpdateTime(String value) {
        UPDATE_TIME = value;
    }

    public static String getUpdateTime() {
        return UPDATE_TIME;
    }

    public static void clear() {
        UPDATE_TIME = null;;
    }
}
