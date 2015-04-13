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
package org.geoserver.gs.mapstoreconfig.ftl.model;

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

    private String minX;

    private String minY;

    private String maxX;

    private String maxY;

    private String minLimit;

    private String maxLimit;

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
     * @return the minX
     */
    public String getMinX() {
        return minX;
    }

    /**
     * @param minX the minX to set
     */
    public void setMinX(String minX) {
        this.minX = minX;
    }

    /**
     * @return the minY
     */
    public String getMinY() {
        return minY;
    }

    /**
     * @param minY the minY to set
     */
    public void setMinY(String minY) {
        this.minY = minY;
    }

    /**
     * @return the maxX
     */
    public String getMaxX() {
        return maxX;
    }

    /**
     * @param maxX the maxX to set
     */
    public void setMaxX(String maxX) {
        this.maxX = maxX;
    }

    /**
     * @return the maxY
     */
    public String getMaxY() {
        return maxY;
    }

    /**
     * @param maxY the maxY to set
     */
    public void setMaxY(String maxY) {
        this.maxY = maxY;
    }

    /**
     * @return the minLimit
     */
    public String getMinLimit() {
        return minLimit;
    }

    /**
     * @param minLimit the minLimit to set
     */
    public void setMinLimit(String minLimit) {
        this.minLimit = minLimit;
    }

    /**
     * @return the maxLimit
     */
    public String getMaxLimit() {
        return maxLimit;
    }

    /**
     * @param maxLimit the maxLimit to set
     */
    public void setMaxLimit(String maxLimit) {
        this.maxLimit = maxLimit;
    }

}
