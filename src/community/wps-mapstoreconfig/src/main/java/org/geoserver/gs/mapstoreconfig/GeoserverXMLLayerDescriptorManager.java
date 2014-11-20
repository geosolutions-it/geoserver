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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.security.auth.login.ConfigurationSpi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess.MapEntryConverter;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess.ResourceConverter;
import org.geoserver.wps.gs.resource.ResourceLoaderProcess.ResourceItemConverter;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.Resources;
import org.geoserver.wps.gs.resource.model.impl.VectorialLayer;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;
import org.geotools.util.logging.Logging;
import org.jaitools.media.jai.vectorize.VectorizeRIF;
import org.opengis.geometry.BoundingBox;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;

/**
 * This class is responsible to manage a Layer Description manager document in XML format
 * 
 * @author DamianoG
 *
 */
public class GeoserverXMLLayerDescriptorManager implements LayerDescriptorManager {

    private static final Logger LOGGER = Logging.getLogger(GeoserverXMLLayerDescriptorManager.class);
    
    private final Catalog catalog;
    
    private BoundingBox bbox; 
    private String minTime;
    private String maxTime;
    private boolean forceDefaultValuesUsage;
    
    private boolean isValid;
    private boolean isValidated;
    
    private Resources document;
    
    public GeoserverXMLLayerDescriptorManager(Catalog catalog){
        this.isValidated = false;
        this.isValid = false;
        this.catalog = catalog;
    }
    
    public void setBbox(BoundingBox bbox) {
        this.bbox = bbox;
    }

    public void setMinTime(String minTime) {
        this.minTime = minTime;
    }

    public void setMaxTime(String maxTime) {
        this.maxTime = maxTime;
    }

    public void setForceDefaultValuesUsage(boolean forceDefaultValuesUsage) {
        this.forceDefaultValuesUsage = forceDefaultValuesUsage;
    }

    public void loadDocument(String xmlDocument, boolean forceReload){
        isValidated = false;
        isValid = false;
        if(document != null && !forceReload){
            LOGGER.warning("loadDocument method doesn't have any effect since the document has been already loaded and force reload is set to false...");
            return;
        }
        XStream xs = initialize();
        document = (Resources) xs.fromXML(xmlDocument);
    }
    
    public boolean validateDocument(){
        if(isValidated){
            return isValid;
        }
        return true;
    }
    
    public List<LayerTemplateModel> produceModelForFTLTemplate(TemplateDirLoader templateDirLoader) throws IOException{
        
        List<LayerTemplateModel> list = new ArrayList<>();
        InputStream input = null;
        try {
            File dir = templateDirLoader.getTemplateDir();
            Properties prop = new Properties();
            // Load From the GeoserverDataDir (the template directory) a file with the Default values to put in the configuration
            // WARNING don't confuse these default values with the default WPS process input parameters
            input = new FileInputStream(MapstoreConfigProcess.DEFAULT_PROPERTIES_NAME);
            prop.load(input);
            for (Resource r : document.getResources()) {
                if (r instanceof VectorialLayer) {
                    LayerTemplateModel ltm = mapVectorLayer(r, prop);
                    list.add(ltm);
                }
            }
        } catch (IOException e) {
            throw e;
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    throw e;
                }
            }
        }
        return list;
    }
    
    public String mimeFormatHandled(){
        return "application/xml";
    }
    
    /**
     * Initialize an XStream Context to perform XML de-serialization of the layerDescriptor document 
     * 
     * @return an XStream initialized context
     * @throws IllegalArgumentException
     */
    protected XStream initialize() throws IllegalArgumentException {
        XStreamPersisterFactory xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStream xs = xpf.createXMLPersister().getXStream();

        // Aliases
        xs.alias("resources", Resources.class);
        xs.alias("resource", Resource.class);
        xs.aliasField("abstract", Resource.class, "abstractTxt");
        xs.aliasField("translateContext", Resource.class, "translateContext");
        xs.aliasAttribute(Resource.class, "type", "class");

        xs.alias("nativeBoundingBox", Map.class);

        xs.alias("defaultStyle", Map.class);
        xs.alias("metadata", Map.class);

        xs.alias("translateContext", TranslateContext.class);
        xs.alias("item", TranslateItem.class);
        xs.aliasAttribute(TranslateItem.class, "type", "class");
        xs.aliasAttribute(TranslateItem.class, "order", "order");

        // Converters
        xs.addImplicitCollection(Resources.class, "resources");
        xs.addImplicitCollection(TranslateContext.class, "items");

        xs.registerConverter(new MapEntryConverter());
        xs.registerConverter(new ResourceConverter(this.catalog));
        xs.registerConverter(new ResourceItemConverter());
        xs.registerConverter(new ReflectionConverter(xs.getMapper(),
                new PureJavaReflectionProvider()), XStream.PRIORITY_VERY_LOW);

        return xs;
    }
    
    public LayerTemplateModel mapVectorLayer(Resource r, Properties prop){
        VectorialLayer vl = (VectorialLayer) r;
        LayerTemplateModel layerValues = new LayerTemplateModel();
        DimensionsTemplateModel timeDimensions = new DimensionsTemplateModel();
        layerValues.setTimeDimensions(timeDimensions);
        
        layerValues.setFormat(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setFixed(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setGroup(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setName(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setOpacity(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setSelected(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setStyles(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setSource(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setTitle(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setVisibility(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setTransparent(StringUtils.defaultString(prop.getProperty("")));
        layerValues.setQueryable(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setCurrent(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setDefaultVal(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setLlbbox(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setMultipleVal(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setName(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setNearestVal(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setUnits(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setUnitsymbol(StringUtils.defaultString(prop.getProperty("")));
        timeDimensions.setValues(StringUtils.defaultString(prop.getProperty("")));
        return layerValues;
    }

}
