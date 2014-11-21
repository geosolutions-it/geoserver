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

import java.io.IOException;
import java.util.List;

import org.geoserver.gs.mapstoreconfig.ftl.model.LayerTemplateModel;
import org.opengis.geometry.BoundingBox;

/**
 * @author DamianoG
 *
 */
public interface LayerDescriptorManager {

    /**
     * @param bbox the bbox to set
     */
    public void setBbox(BoundingBox bbox);

    /**
     * @param minTime the minTime to set
     */
    public void setMinTime(String minTime);

    /**
     * @param maxTime the maxTime to set
     */
    public void setMaxTime(String maxTime);

    /**
     * @param forceDefaultValuesUsage the forceDefaultValuesUsage to set
     */
    public void setForceDefaultValuesUsage(boolean forceDefaultValuesUsage);

    public void loadDocument(String document, boolean forceReload);

    public boolean validateDocument();

    public List<LayerTemplateModel> produceModelForFTLTemplate(TemplateDirLoader templateDirLoader) throws IOException;

    public String mimeFormatHandled();

}