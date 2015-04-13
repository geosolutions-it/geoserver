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

import java.io.File;
import java.io.IOException;

import org.geotools.test.TestData;

/**
 * @author DamianoG
 *
 */
public class TestTemplateDirLoader  implements TemplateDirLoader {

    // The path related to gs datadir in which search for templates
    private final static String TEMPLATE_DIR = "templates";
    
    @Override
    public File getTemplateDir() throws IOException{
        try {
            return TestData.file(this, TEMPLATE_DIR);
        } catch (Exception e) {
            throw new IOException("Error occurred while loading the template dir");
        }
    }

}
