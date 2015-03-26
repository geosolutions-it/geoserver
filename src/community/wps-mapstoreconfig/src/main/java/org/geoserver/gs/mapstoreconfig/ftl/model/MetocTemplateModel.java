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
public class MetocTemplateModel {

    private String sourceId;
    
    private String title;
    
    private String owsBaseURL;

    private String owsService;

    private String owsVersion;

    private String owsResourceIdentifier;

    private String referenceTimeDim;

    /**
     * @return the sourceId
     */
    public String getSourceId() {
        return sourceId;
    }

    /**
     * @param sourceId the sourceId to set
     */
    public void setSourceId(String sourceId) {
        this.sourceId = sourceId;
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
     * @return the owsBaseURL
     */
    public String getOwsBaseURL() {
        return owsBaseURL;
    }

    /**
     * @param owsBaseURL the owsBaseURL to set
     */
    public void setOwsBaseURL(String owsBaseURL) {
        this.owsBaseURL = owsBaseURL;
    }

    /**
     * @return the owsService
     */
    public String getOwsService() {
        return owsService;
    }

    /**
     * @param owsService the owsService to set
     */
    public void setOwsService(String owsService) {
        this.owsService = owsService;
    }

    /**
     * @return the owsVersion
     */
    public String getOwsVersion() {
        return owsVersion;
    }

    /**
     * @param owsVersion the owsVersion to set
     */
    public void setOwsVersion(String owsVersion) {
        this.owsVersion = owsVersion;
    }

    /**
     * @return the owsResourceIdentifier
     */
    public String getOwsResourceIdentifier() {
        return owsResourceIdentifier;
    }

    /**
     * @param owsResourceIdentifier the owsResourceIdentifier to set
     */
    public void setOwsResourceIdentifier(String owsResourceIdentifier) {
        this.owsResourceIdentifier = owsResourceIdentifier;
    }

    /**
     * @return the referenceTimeDim
     */
    public String getReferenceTimeDim() {
        return referenceTimeDim;
    }

    /**
     * @param referenceTimeDim the referenceTimeDim to set
     */
    public void setReferenceTimeDim(String referenceTimeDim) {
        this.referenceTimeDim = referenceTimeDim;
    }


}
