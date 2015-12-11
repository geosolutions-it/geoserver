/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wps.gs.resource.model.Dimension;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.Resources;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;
import org.geoserver.wps.gs.resource.model.translate.TranslateItemConverter;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.StringRawData;
import org.geoserver.wps.resource.WPSResourceManager;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * The process takes an XML defining the Resources to load and runs the TranlationContext state machine on each one. Every ResourceLoader and Resource
 * know how to handle the import process. The ones integrated with GeoServer, like the VectorialLayer and RasterLayer, basically do a Importer
 * Extension wrap in order to perform the transformation the import on the catalog.
 * 
 * @author alessio.fabiani
 * 
 */
@SuppressWarnings("deprecation")
@DescribeProcess(title = "Resource Loader Process", description = "Downloads Layer Stream and provides a ZIP.")
public class ResourceLoaderProcess implements GSProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(ResourceLoaderProcess.class);

    /** The catalog. */
    private final Catalog catalog;

    private WPSResourceManager resourceManager;

    public ResourceLoaderProcess(GeoServer geoServer, WPSResourceManager resourceManager) {
        Utilities.ensureNonNull("geoServer", geoServer);
        this.catalog = geoServer.getCatalog();
        this.resourceManager = resourceManager;
    }

    @DescribeResult(name = "result", description = "XML describing the Resources to load")
    public String execute(
            @DescribeParameter(name = "resourcesXML", min = 1, description = "XML describing the Resources to load", meta = {
                    "mimeTypes=application/json,text/xml" }) RawData resourcesXML,
            final ProgressListener progressListener) throws ProcessException {

        // Initialize Unmarshaller
        XStream xs = initialize(catalog);
        final String resourceDescriptor = ((StringRawData) resourcesXML).getData();

        // De-serialize resources
        try {
            Resources resources = (Resources) xs.fromXML(resourceDescriptor);

            // Create-or-update the resources
            for (Resource resource : resources.getResources()) {

                // Sanity Checks
                if (!resource.isWellDefined()) {
                    throw new IllegalArgumentException(
                            "The resources definition were not well formed.");
                }

                if (resource.getTranslateContext() != null) {
                    resource.getTranslateContext().run();
                }
            }

            // Store XML into the WPS folder
            // storeResourcesXML(xs, resources);
            storeResourcesXML(resourceDescriptor);
        } catch (Exception cause) {
            throw new ProcessException(cause);
        }

        return resourceDescriptor;
    }

    /**
     * @param catalog2
     * @return
     * @throws IllegalArgumentException
     */
    public static XStream initialize(Catalog catalog) throws IllegalArgumentException {
        XStreamPersisterFactory xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        XStream xs = xpf.createXMLPersister().getXStream();

        // Aliases
        xs.alias("resources", Resources.class);
        xs.alias("resource", Resource.class);
        xs.alias("dimension", Dimension.class);
        xs.alias("dimensionInfo", DimensionInfo.class);

        xs.aliasField("abstract", Resource.class, "abstractTxt");
        xs.aliasField("dimensionInfo", Dimension.class, "dimensionInfo");
        xs.aliasField("translateContext", Resource.class, "translateContext");

        xs.aliasAttribute(Resource.class, "type", "class");

        xs.alias("nativeBoundingBox", Map.class);
        xs.alias("latLonBoundingBox", Map.class);

        xs.alias("defaultStyle", Map.class);
        xs.alias("metadata", Map.class);

        xs.alias("translateContext", TranslateContext.class);
        xs.alias("item", TranslateItem.class);
        xs.aliasAttribute(TranslateItem.class, "type", "class");
        xs.aliasAttribute(TranslateItem.class, "order", "order");

        // Default Implementations
        xs.addDefaultImplementation(DimensionInfoImpl.class, DimensionInfo.class);

        // Converters
        xs.addImplicitCollection(Resources.class, "resources");
        xs.addImplicitCollection(Resource.class, "dimensions");
        xs.addImplicitCollection(TranslateContext.class, "items");

        xs.registerConverter(new MapEntryConverter());
        xs.registerConverter(new ResourceConverter(catalog));
        xs.registerConverter(new ResourceItemConverter());
        xs.registerConverter(
                new ReflectionConverter(xs.getMapper(), new PureJavaReflectionProvider()),
                XStream.PRIORITY_VERY_LOW);

        return xs;
    }

    /**
     * @param xs
     * @param resources
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void storeResourcesXML(XStream xs, Resources resources)
            throws IllegalArgumentException, IOException, FileNotFoundException {
        final String executionId = resourceManager.getExecutionId(null);
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File wpsProcessFolder = new File(loader.getBaseDirectory().getCanonicalPath(),
                "/temp/wps/" + executionId);

        if (!wpsProcessFolder.exists()) {
            wpsProcessFolder.mkdirs();
        }

        if (wpsProcessFolder.exists() && wpsProcessFolder.isDirectory()) {
            final File wpsResourceLoaderOutput = new File(wpsProcessFolder,
                    "resources_" + executionId + ".xml");
            final FileOutputStream fos = new FileOutputStream(wpsResourceLoaderOutput);
            try {
                xs.toXML(resources, fos);
            } catch (Exception cause) {
                LOGGER.log(Level.SEVERE, "Could not marshall Resources.", cause);
            } finally {
                IOUtils.closeQuietly(fos);
            }
        }
    }

    /**
     * 
     * @param data
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void storeResourcesXML(String data)
            throws IllegalArgumentException, IOException, FileNotFoundException {
        final String executionId = resourceManager.getExecutionId(null);
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        File wpsProcessFolder = new File(loader.getBaseDirectory().getCanonicalPath(),
                "/temp/wps/" + executionId);

        if (!wpsProcessFolder.exists()) {
            wpsProcessFolder.mkdirs();
        }

        if (wpsProcessFolder.exists() && wpsProcessFolder.isDirectory()) {
            final File wpsResourceLoaderOutput = new File(wpsProcessFolder,
                    "resources_" + executionId + ".xml");
            try {
                FileUtils.writeStringToFile(wpsResourceLoaderOutput, data, "UTF-8", false);
            } catch (Exception cause) {
                LOGGER.log(Level.SEVERE, "Could not marshall Resources.", cause);
            } finally {
            }
        }
    }

    /**
     * This XStream converter performs the marshalling and unmarshalling of a {@link java.util.Map} into an XML having the nodes named like the Map
     * keys.
     * 
     * @author alessio.fabiani
     * 
     */
    public static class MapEntryConverter implements Converter {

        @Override
        public boolean canConvert(Class clazz) {
            return AbstractMap.class.isAssignableFrom(clazz);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            AbstractMap map = (AbstractMap) value;
            for (Object obj : map.entrySet()) {
                Map.Entry entry = (Map.Entry) obj;
                writer.startNode(entry.getKey().toString().replaceAll(" ", "___"));
                writer.setValue(entry.getValue().toString());
                writer.endNode();
            }

        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {

            Map<String, Object> map = new HashMap<String, Object>();

            while (reader.hasMoreChildren()) {
                reader.moveDown();

                String key = reader.getNodeName(); // nodeName aka element's name
                key = key.replaceAll("___", " ");
                Object value = reader.getValue();
                if (AbstractMap.class.isAssignableFrom(value.getClass()))
                    value = unmarshal(reader, context);
                map.put(key, value);

                reader.moveUp();
            }

            return map;
        }

    }

    /**
     * This XStream converter performs a scan of the GeoServer Extension capable of converting the Resource defined into the XML. The converter than
     * delegates the conversion responsibility to the selected extension.
     * 
     * @author alessio.fabiani
     * 
     */
    public static class ResourceConverter implements Converter {

        private Catalog catalog;

        /**
         * 
         * @param geoServer
         * @param resourceManager
         */
        public ResourceConverter(Catalog catalog) {
            this.catalog = catalog;
        }

        @Override
        public boolean canConvert(Class clazz) {
            return Resource.class.isAssignableFrom(clazz);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            for (ResourceLoaderConverter extension : GeoServerExtensions
                    .extensions(ResourceLoaderConverter.class)) {
                if (extension.canConvert(value.getClass())) {
                    extension.marshal(value, writer, context);
                    break;
                }
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            final String type = reader.getAttribute("class");

            for (ResourceLoaderConverter extension : GeoServerExtensions
                    .extensions(ResourceLoaderConverter.class)) {
                if (extension.getTYPE().equals(type)) {
                    Resource resource = (Resource) extension.unmarshal(reader, context);
                    resource.setType(type);

                    /** Set the Translate Context Resources */
                    if (resource.getTranslateContext() != null) {
                        resource.getTranslateContext().setCatalog(catalog);
                        resource.getTranslateContext().setOriginator(resource);
                    }

                    return resource;
                }
            }

            return null;
        }

    }

    /**
     * Acts similarly to the {@link ResourceConverter} excepts that it searches for the Resources Translation Items.
     * 
     * @author alessio.fabiani
     * 
     */
    public static class ResourceItemConverter implements Converter {

        @Override
        public boolean canConvert(Class clazz) {
            return TranslateItem.class.isAssignableFrom(clazz);
        }

        @Override
        public void marshal(Object value, HierarchicalStreamWriter writer,
                MarshallingContext context) {

            for (TranslateItemConverter extension : GeoServerExtensions
                    .extensions(TranslateItemConverter.class)) {
                if (extension.canConvert(value.getClass())) {
                    extension.marshal(value, writer, context);
                    break;
                }
            }
        }

        @Override
        public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
            final String type = reader.getAttribute("class");

            for (TranslateItemConverter extension : GeoServerExtensions
                    .extensions(TranslateItemConverter.class)) {
                if (extension.getTYPE().equals(type)) {
                    TranslateItem translateItem = (TranslateItem) extension.unmarshal(reader,
                            context);
                    translateItem.setType(type);
                    return translateItem;
                }
            }

            return null;
        }

    }
}
