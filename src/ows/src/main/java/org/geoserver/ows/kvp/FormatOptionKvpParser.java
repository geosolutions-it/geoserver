/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.ows.kvp;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.ows.KvpParser;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * General purpose parser for parameters
 * 
 * @author carlo cancellieri - GeoSolutions
 * 
 */
public class FormatOptionKvpParser implements ApplicationContextAware {
    /**
     * application context used to lookup KvpParsers
     */
    ApplicationContext applicationContext;

    final static Map<String, KvpParser> parsers = new HashMap<String, KvpParser>();

    /**
     * Builds a generic {@link FormatOptionKvpParser}
     * 
     * @param key
     */
    public FormatOptionKvpParser() {
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;

        for (KvpParser p : GeoServerExtensions.extensions(KvpParser.class, applicationContext)) {
            parsers.put(p.getKey(), p);
        }

    }

    public Object parse(String key, String value) throws Exception {
        Object parsed = null;
        KvpParser parser = parsers.get(key);
        if (parser == null) {
            // if(LOGGER.isLoggable(Level.FINER))
            // LOGGER.finer( "Could not find parser for: '" + key + "'. Storing as raw string.");
            return value;
        }
        if (key.equalsIgnoreCase(parser.getKey())) {
            parsed = parser.parse(value);
        }
        if (parsed == null) {
            // if(LOGGER.isLoggable(Level.FINER))
            // LOGGER.finer( "Could not find parser for: '" + key + "'. Storing as raw string.");
            return value;
        }
        return parsed;
    }
}
