/**
 * 
 */
package org.geoserver.wps.gs.resource.model;

import java.io.Serializable;
import java.util.logging.Logger;

import org.geoserver.catalog.DimensionInfo;
import org.geotools.util.logging.Logging;

/**
 * @author alessio.fabiani
 *
 */
public class Dimension implements Serializable {

	static protected Logger LOGGER = Logging.getLogger(Dimension.class);

    private String name;
    
    private String description;
    
    private String min;
    
    private String max;
    
    private DimensionInfo dimensionInfo;

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the min
	 */
	public String getMin() {
		return min;
	}

	/**
	 * @param min the min to set
	 */
	public void setMin(String min) {
		this.min = min;
	}

	/**
	 * @return the max
	 */
	public String getMax() {
		return max;
	}

	/**
	 * @param max the max to set
	 */
	public void setMax(String max) {
		this.max = max;
	}

	/**
	 * @return the dimensionInfo
	 */
	public DimensionInfo getDimensionInfo() {
		return dimensionInfo;
	}

	/**
	 * @param dimensionInfo the dimensionInfo to set
	 */
	public void setDimensionInfo(DimensionInfo dimensionInfo) {
		this.dimensionInfo = dimensionInfo;
	}
    
    
}
