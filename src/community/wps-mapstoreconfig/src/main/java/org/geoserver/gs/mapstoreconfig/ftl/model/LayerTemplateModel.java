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
public class LayerTemplateModel {

    private String format;
    
    private String fixed;

    private String group;

    private String name;

    private String opacity;

    private String selected;

    private String source;
    
    private String styles;

    private String title;

    private String visibility;
    
    private String transparent;

    private String queryable;
    
    private DimensionsTemplateModel timeDimensions;

    /**
     * @return the format
     */
    public String getFormat() {
        return format;
    }

    /**
     * @param format the format to set
     */
    public void setFormat(String format) {
        this.format = format;
    }

    /**
     * @return the fixed
     */
    public String getFixed() {
        return fixed;
    }

    /**
     * @param fixed the fixed to set
     */
    public void setFixed(String fixed) {
        this.fixed = fixed;
    }

    /**
     * @return the group
     */
    public String getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(String group) {
        this.group = group;
    }

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
     * @return the opacity
     */
    public String getOpacity() {
        return opacity;
    }

    /**
     * @param opacity the opacity to set
     */
    public void setOpacity(String opacity) {
        this.opacity = opacity;
    }

    /**
     * @return the selected
     */
    public String getSelected() {
        return selected;
    }

    /**
     * @param selected the selected to set
     */
    public void setSelected(String selected) {
        this.selected = selected;
    }

    /**
     * @return the source
     */
    public String getSource() {
        return source;
    }

    /**
     * @param source the source to set
     */
    public void setSource(String source) {
        this.source = source;
    }

    /**
     * @return the styles
     */
    public String getStyles() {
        return styles;
    }

    /**
     * @param styles the styles to set
     */
    public void setStyles(String styles) {
        this.styles = styles;
    }

    /**
     * @return the title
     */
    public String getTitle() {
        return title;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return the visibility
     */
    public String getVisibility() {
        return visibility;
    }

    /**
     * @param visibility the visibility to set
     */
    public void setVisibility(String visibility) {
        this.visibility = visibility;
    }

    /**
     * @return the transparent
     */
    public String getTransparent() {
        return transparent;
    }

    /**
     * @param transparent the transparent to set
     */
    public void setTransparent(String transparent) {
        this.transparent = transparent;
    }

    /**
     * @return the timeDimensions
     */
    public DimensionsTemplateModel getTimeDimensions() {
        return timeDimensions;
    }

    /**
     * @param timeDimensions the timeDimensions to set
     */
    public void setTimeDimensions(DimensionsTemplateModel timeDimensions) {
        this.timeDimensions = timeDimensions;
    }

    /**
     * @return the queryable
     */
    public String getQueryable() {
        return queryable;
    }

    /**
     * @param queryable the queryable to set
     */
    public void setQueryable(String queryable) {
        this.queryable = queryable;
    }

    
}
