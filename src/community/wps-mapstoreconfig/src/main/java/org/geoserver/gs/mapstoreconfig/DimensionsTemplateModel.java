/*
 *  Copyright (C) 2007-2012 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.gs.mapstoreconfig;

/**
 * @author DamianoG
 * 
 */
public class DimensionsTemplateModel {

    private String name;

    private String units;

    private String unitsymbol;

    private String nearestVal;

    private String multipleVal;

    private String current;

    private String defaultVal;

    private String values;

    private String llbbox;

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
     * @return the units
     */
    public String getUnits() {
        return units;
    }

    /**
     * @param units the units to set
     */
    public void setUnits(String units) {
        this.units = units;
    }

    /**
     * @return the unitsymbol
     */
    public String getUnitsymbol() {
        return unitsymbol;
    }

    /**
     * @param unitsymbol the unitsymbol to set
     */
    public void setUnitsymbol(String unitsymbol) {
        this.unitsymbol = unitsymbol;
    }

    /**
     * @return the nearestVal
     */
    public String getNearestVal() {
        return nearestVal;
    }

    /**
     * @param nearestVal the nearestVal to set
     */
    public void setNearestVal(String nearestVal) {
        this.nearestVal = nearestVal;
    }

    /**
     * @return the multipleVal
     */
    public String getMultipleVal() {
        return multipleVal;
    }

    /**
     * @param multipleVal the multipleVal to set
     */
    public void setMultipleVal(String multipleVal) {
        this.multipleVal = multipleVal;
    }

    /**
     * @return the current
     */
    public String getCurrent() {
        return current;
    }

    /**
     * @param current the current to set
     */
    public void setCurrent(String current) {
        this.current = current;
    }

    /**
     * @return the defaultVal
     */
    public String getDefaultVal() {
        return defaultVal;
    }

    /**
     * @param defaultVal the defaultVal to set
     */
    public void setDefaultVal(String defaultVal) {
        this.defaultVal = defaultVal;
    }

    /**
     * @return the values
     */
    public String getValues() {
        return values;
    }

    /**
     * @param values the values to set
     */
    public void setValues(String values) {
        this.values = values;
    }

    /**
     * @return the llbbox
     */
    public String getLlbbox() {
        return llbbox;
    }

    /**
     * @param llbbox the llbbox to set
     */
    public void setLlbbox(String llbbox) {
        this.llbbox = llbbox;
    }

}
