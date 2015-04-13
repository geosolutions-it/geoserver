/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.transform;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.geoserver.importer.ImportTask;
import org.geoserver.importer.ValidationException;
import org.geotools.data.DataStore;
import org.opengis.feature.simple.SimpleFeature;

/**
 * Transform that converts an epoch number field in a date attribute.
 * This class is not thread-safe.
 *
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class EpochToDateTransform extends AttributeRemapTransform {
    
    private static final long serialVersionUID = 1L;

    private final long epochMux;

    public EpochToDateTransform(String field, long epochMux) throws ValidationException  {
    	this.epochMux = epochMux;
        init(field);
        init();
    }
    
    EpochToDateTransform() {
        this(null, 1);
    }

    private void init(String field) throws ValidationException {
        setType(Date.class);
        setField(field);
    }

    /**
	 * @return the epochMux
	 */
	public long getEpochMux() {
		return epochMux;
	}

	@Override
    public SimpleFeature apply(ImportTask task, DataStore dataStore, SimpleFeature oldFeature,
            SimpleFeature feature) throws Exception {
        Object val = oldFeature.getAttribute(field);
        if (val != null) {
        	long epoch = 1;
        	if (val instanceof String) {
        		epoch = Long.parseLong((String) val);
        	} else if (val instanceof Integer) {
        		epoch = (Integer) val;
        	} else {
        		epoch = (long) val;
        	}
        	epoch = epoch * epochMux;
        	
        	TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        	Calendar c = new GregorianCalendar();
        	c.setTimeInMillis(epoch);
        	Date parsed = c.getTime();
        	feature.setAttribute(field, parsed);
        }
        return feature;
    }

}
