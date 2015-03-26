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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.geoserver.gs.mapstoreconfig.components.GeoserverXMLLayerDescriptorManager;
import org.geoserver.gs.mapstoreconfig.ftl.model.DimensionsTemplateModel;
import org.geoserver.gs.mapstoreconfig.ftl.model.LayerTemplateModel;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.test.TestData;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;

/**
 * @author DamianoG
 * 
 */
// @RunWith(SpringJUnit4ClassRunner.class)
// @ContextConfiguration(locations = "classpath:/applicationContext-test.xml")
public class MapstoreConfigTest extends WPSTestSupport {

    private static final Logger LOGGER = Logging.getLogger(MapstoreConfigTest.class);

    public MapstoreConfigProcess mapstoreProcess;

    @Test
    // @Ignore
    public void basicTest() throws NoSuchAuthorityCodeException, FactoryException,
            FileNotFoundException, IOException {
        mapstoreProcess = new MapstoreConfigProcess();
        LayerDescriptorManager ldm = new GeoserverXMLLayerDescriptorManager(getGeoServer()
                .getCatalog());
        mapstoreProcess.setLayerDescriptorManager(ldm);
        mapstoreProcess.setTemplateDirLoader(new TestTemplateDirLoader());
        String metocs = "[{\"Metoc\": {\"sourceId\":\"karpathos-dev\",\"title\":\"NOAA Wave Height\",\"owsBaseURL\":\"http://karpathos-dev/geoserver/ows\",\"owsService\":\"WMS\",\"owsVersion\":\"1.3.0\",\"owsResourceIdentifier\":\"oceanmod:NOAAWaveHeight\",\"referenceTimeDim\":true}},{\"Metoc\": {\"sourceId\":\"karpathos-dev\",\"title\":\"NOAA Wind Speed\",\"owsBaseURL\":\"http://karpathos-dev/geoserver/ows\",\"owsService\":\"WMS\",\"owsVersion\":\"1.3.0\",\"owsResourceIdentifier\":\"oceanmod:NOAAWindSpeed\",\"referenceTimeDim\":true}}]";
        LOGGER.info(mapstoreProcess.execute(metocs, getLayerDescriptor()));
    }

    private String getLayerDescriptor() throws FileNotFoundException, IOException {
        File xmlDoc = TestData.file(this, "layerDescriptor.xml");
        return IOUtils.toString(new FileInputStream(xmlDoc));
    }

    public static List<LayerTemplateModel> produceModel() {
        List<LayerTemplateModel> list = new ArrayList<>();

        LayerTemplateModel layerValues1 = new LayerTemplateModel();
        DimensionsTemplateModel timeDimensions1 = new DimensionsTemplateModel();
        layerValues1.setTimeDimensions(timeDimensions1);
        layerValues1.setFormat("Format1");
        layerValues1.setFixed("Fixed1");
        layerValues1.setGroup("Group1");
        layerValues1.setName("Name1");
        layerValues1.setOpacity("Opacity1");
        layerValues1.setSelected("Selected1");
        layerValues1.setStyles("Style1");
        layerValues1.setSource("Source1");
        layerValues1.setTitle("Title1");
        layerValues1.setVisibility("Visibility1");
        layerValues1.setTransparent("Trasparent1");
        layerValues1.setQueryable("Queryable1");
        timeDimensions1.setCurrent("Current1");
        timeDimensions1.setDefaultVal("DefaultVal1");
        timeDimensions1.setMinX("minX");
        timeDimensions1.setMinY("minY");
        timeDimensions1.setMaxX("maxX");
        timeDimensions1.setMaxY("maxY");
        timeDimensions1.setMultipleVal("MultipleVal1");
        timeDimensions1.setName("Name1");
        timeDimensions1.setNearestVal("NearestVal1");
        timeDimensions1.setUnits("Units1");
        timeDimensions1.setUnitsymbol("Unitsymbol1");
        timeDimensions1.setMinLimit("minLimit");
        timeDimensions1.setMaxLimit("maxLimit");

        LayerTemplateModel layerValues2 = new LayerTemplateModel();
        DimensionsTemplateModel timeDimensions2 = new DimensionsTemplateModel();
        layerValues2.setTimeDimensions(timeDimensions2);
        layerValues2.setFormat("Format1");
        layerValues2.setFixed("Fixed2");
        layerValues2.setGroup("Group2");
        layerValues2.setName("Name2");
        layerValues2.setOpacity("Opacity2");
        layerValues2.setSelected("Selected2");
        layerValues2.setStyles("Style1");
        layerValues2.setSource("Source2");
        layerValues2.setTitle("Title2");
        layerValues2.setVisibility("Visibility2");
        layerValues2.setTransparent("Trasparent2");
        layerValues2.setQueryable("Queryable2");
        timeDimensions2.setCurrent("Current2");
        timeDimensions2.setDefaultVal("DefaultVal2");
        timeDimensions2.setMinX("minX");
        timeDimensions2.setMinY("minY");
        timeDimensions2.setMaxX("maxX");
        timeDimensions2.setMaxY("maxY");
        timeDimensions2.setMultipleVal("MultipleVal2");
        timeDimensions2.setName("Name2");
        timeDimensions2.setNearestVal("NearestVal2");
        timeDimensions2.setUnits("Units2");
        timeDimensions2.setUnitsymbol("Unitsymbol2");
        timeDimensions2.setMinLimit("minLimit");
        timeDimensions2.setMaxLimit("maxLimit");

        LayerTemplateModel layerValues3 = new LayerTemplateModel();
        DimensionsTemplateModel timeDimensions3 = new DimensionsTemplateModel();
        layerValues3.setTimeDimensions(timeDimensions3);
        layerValues3.setFormat("Format1");
        layerValues3.setFixed("Fixed3");
        layerValues3.setGroup("Group3");
        layerValues3.setName("Name3");
        layerValues3.setOpacity("Opacity3");
        layerValues3.setStyles("Style1");
        layerValues3.setSelected("Selected3");
        layerValues3.setSource("Source3");
        layerValues3.setTitle("Title3");
        layerValues3.setVisibility("Visibility3");
        layerValues3.setTransparent("Trasparent3");
        layerValues3.setQueryable("Queryable3");
        timeDimensions3.setCurrent("Current3");
        timeDimensions3.setDefaultVal("DefaultVal3");
        timeDimensions3.setMinX("minX");
        timeDimensions3.setMinY("minY");
        timeDimensions3.setMaxX("maxX");
        timeDimensions3.setMaxY("maxY");
        timeDimensions3.setMultipleVal("MultipleVal3");
        timeDimensions3.setName("Name3");
        timeDimensions3.setNearestVal("NearestVal3");
        timeDimensions3.setUnits("Units3");
        timeDimensions3.setUnitsymbol("Unitsymbol3");
        timeDimensions3.setMinLimit("minLimit");
        timeDimensions3.setMaxLimit("maxLimit");

        list.add(layerValues1);
        list.add(layerValues2);
        list.add(layerValues3);

        return list;
    }
}
