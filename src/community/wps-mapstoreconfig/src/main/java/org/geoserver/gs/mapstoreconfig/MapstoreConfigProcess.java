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
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.gs.mapstoreconfig.components.GeoserverTemplateDirLoader;
import org.geoserver.gs.mapstoreconfig.ftl.model.MapTemplateModel;
import org.geoserver.gs.mapstoreconfig.ftl.model.MetocTemplateModel;
import org.geoserver.wps.gs.GeoServerProcess;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.StringRawData;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.util.logging.Logging;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;

@DescribeProcess(title = "mapstoreConfigProcess", description = "A wps process responsible for generate Mapstore jsnon configuration files")
public class MapstoreConfigProcess implements GeoServerProcess {

    private static final Logger LOGGER = Logging.getLogger(MapstoreConfigProcess.class);

    private static final int HOURS_TO_SECONDS = 3600;

    /**
     * The name of the template to load
     */
    public final static String TEMPLATE_NAME = "mapstoreTemplate.tpl";

    /**
     * The name of the properties file that stores the default values
     */
    public final static String DEFAULT_PROPERTIES_NAME = "defaultMapstoreConfigValues.tpl";

    /**
     * Interface responsible to load the template dir. Use {@link GeoserverTemplateDirLoader} as implementation to place the template dir inside a
     * Geoserver Datadir
     */
    private TemplateDirLoader templateDirLoader;

    /**
     * This Interface manage a layer descriptor. Different implementation should support different datatype supported (xml, json, plaintext ecc...).
     */
    private LayerDescriptorManager layerDescriptorManager;

    /**
     * @param templateDirLoader the templateDirLoader to set
     */
    public void setTemplateDirLoader(TemplateDirLoader templateDirLoader) {
        this.templateDirLoader = templateDirLoader;
    }

    /**
     * @param layerDescriptorManager the layerDescriptorManager to set
     */
    public void setLayerDescriptorManager(LayerDescriptorManager layerDescriptorManager) {
        this.layerDescriptorManager = layerDescriptorManager;
    }

    @DescribeResult(name = "JSON MapStore configuration file", description = "output result", type = String.class)
    public String execute(
            @DescribeParameter(name = "layerDescriptor", min = 1, description = "An xml document that provides a description of a set of layers") String layerDescriptor,
            @DescribeParameter(name = "metoc", min = 0, description = "List of Metocs used by the RiskMap", meta = { "mimeTypes=application/json,text/xml" }) final RawData metoc)
            throws IOException {

        // Manage the layerDescriptor and produce the value to substitute in the FTL template
        layerDescriptorManager.loadDocument(layerDescriptor, true);
        if (!layerDescriptorManager.validateDocument()) {
            // TODO How handle this? How throw an Exception???
            LOGGER.severe("The provided layerDescriptor Document is not valid for the '"
                    + layerDescriptorManager.mimeFormatHandled() + "' input format...");
        }
        MapTemplateModel model = null;

        // Could maybe throw an exception
        model = layerDescriptorManager.produceModelForFTLTemplate(templateDirLoader);

        // Load the template Location
        File templateDir = null;

        // Could maybe throw an exception
        templateDir = templateDirLoader.getTemplateDir();

        // Setup the FTL Context
        Configuration cfg = new Configuration();

        // Could maybe throw an exception
        // Where do we load the templates from:
        cfg.setDirectoryForTemplateLoading(templateDir);

        cfg.setDefaultEncoding("UTF-8");
        cfg.setLocale(Locale.ENGLISH);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);

        // Set the values to use for the template resolving
        Map<String, Object> input = new HashMap<String, Object>();

        // Extracting Metocs from JSON
        List<MetocTemplateModel> metocs = new ArrayList<MetocTemplateModel>();
        
        if (metoc != null && metoc instanceof StringRawData && ((StringRawData)metoc).getData() != null) {
            JsonFactory jsonF = new JsonFactory();
            JsonParser jsonP = jsonF.createParser(((StringRawData)metoc).getData());
            jsonP.nextToken(); // will return JsonToken.START_ARRAY
            while (jsonP.nextToken() != JsonToken.END_ARRAY) {
                jsonP.nextToken(); // move to value, or START_OBJECT/START_ARRAY

                JsonToken token = jsonP.getCurrentToken();
                if (token == JsonToken.END_ARRAY) break;
                
                String fieldname = jsonP.getCurrentName();

                // Parsing Metocs
                if (fieldname != null && fieldname.equalsIgnoreCase("Metoc")) {
                    jsonP.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                    token = jsonP.getCurrentToken();
                    MetocTemplateModel metocObj = extractJsonProperties(jsonP);

                    metocs.add(metocObj);
                }
            }

            jsonP.close();
        }
        
        model.setMetocs(metocs);

        // input.put("layers", MapstoreConfigTest.produceModel());
        input.put("map", model);

        // Load the FTL template
        // Could maybe throw an exception
        Template template = cfg.getTemplate(TEMPLATE_NAME);

        // Resolve the template
        // Could maybe throw an exception
        Writer writer = new StringWriter();
        try {
            template.process(input, writer);
        } catch (TemplateException e) {
            throw new IOException(
                    "An instance of TemplateException as been thrown, reporting its mesage: '"
                            + e.getMessage() + "'");
        }

        return writer.toString();
    }

    /**
     * @param jsonP
     * @return
     * @throws IOException
     * @throws JsonParseException
     */
    private static MetocTemplateModel extractJsonProperties(JsonParser jsonP) throws IOException,
            JsonParseException {
        ObjectMapper mapper = new ObjectMapper();
        MetocTemplateModel metoc = mapper.readValue(jsonP, MetocTemplateModel.class);
        
//        StringBuilder metoc = new StringBuilder();
//
//        metoc.append("{");
//
//        while (jsonP.nextToken() != JsonToken.END_OBJECT) {
//            String propertyname = jsonP.getCurrentName();
//            jsonP.nextToken(); // move to value, or START_OBJECT/START_ARRAY
//
//            if (jsonP.getCurrentToken() != JsonToken.START_OBJECT) {
//
//                metoc.append("\"").append(propertyname).append("\":");
//
//                final String value = jsonP.getText();
//                if (!isNumeric(value))
//                    metoc.append("\"");
//                metoc.append(value);
//                if (!isNumeric(value))
//                    metoc.append("\"");
//                metoc.append(",");
//            } else if (jsonP.getCurrentToken() == JsonToken.START_OBJECT) {
//
//                metoc.append("{\"").append(propertyname).append("\": {");
//
//                while (jsonP.nextToken() != JsonToken.END_OBJECT) {
//                    propertyname = jsonP.getCurrentName();
//                    jsonP.nextToken(); // move to value, or START_OBJECT/START_ARRAY
//
//                    metoc.append("\"").append(propertyname).append("\":");
//
//                    final String value = jsonP.getText();
//                    if (!isNumeric(value))
//                        metoc.append("\"");
//                    metoc.append(value);
//                    if (!isNumeric(value))
//                        metoc.append("\"");
//                    metoc.append(",");
//                }
//
//                metoc.deleteCharAt(metoc.length() - 1);
//                metoc.append("}");
//            }
//        }
//
//        metoc.deleteCharAt(metoc.length() - 1);
//        metoc.append("}");

        return metoc;
    }

    /**
     * 
     * @param value
     * @return
     */
    private static boolean isNumeric(String value) {
        boolean isNumber = false;

        try {
            Double.parseDouble(value);
            isNumber = true;
        } catch (Exception e) {
            isNumber = false;
        }

        return isNumber;
    }
}