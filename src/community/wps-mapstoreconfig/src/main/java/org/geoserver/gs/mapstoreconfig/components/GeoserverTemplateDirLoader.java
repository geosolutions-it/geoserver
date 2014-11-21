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
package org.geoserver.gs.mapstoreconfig.components;

import java.io.File;

import org.geoserver.gs.mapstoreconfig.TemplateDirLoader;
import org.geoserver.platform.GeoServerResourceLoader;

/**
 * @author DamianoG
 * 
 */
public class GeoserverTemplateDirLoader implements TemplateDirLoader {

    private GeoServerResourceLoader loader;
    
    // The path related to gs datadir in which search for templates
    private final static String TEMPLATE_DIR = "templates";

    /**
     * @param loader the loader to set
     */
    public void setLoader(GeoServerResourceLoader loader) {
        this.loader = loader;
    }

    public GeoserverTemplateDirLoader(){}
    
    @Override
    public File getTemplateDir() {
        return new File(loader.getBaseDirectory(),TEMPLATE_DIR);
    }
}
