/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

/**
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 *
 */
public class ConfigRequest {

    static volatile Class REQUEST;
    static volatile Class CURRENT;

    public static void start(Class request) {
        REQUEST = request;
    }

    public static void request(Class request) {
        CURRENT = request;
    }

    public static void abort() {
        REQUEST = null;
        CURRENT = null;
    }

    public static Object get() {
        return REQUEST;
    }

    public static Object current() {
        return CURRENT;
    }

    public static void finish() {
        REQUEST = null;
        CURRENT = null;
    }
    
}
