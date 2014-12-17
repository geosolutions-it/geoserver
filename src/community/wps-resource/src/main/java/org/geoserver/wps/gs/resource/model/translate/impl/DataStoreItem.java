/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.importer.DataFormat;
import org.geoserver.importer.Database;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.UpdateMode;
import org.geoserver.importer.csv.CSVDataStoreFactory;
import org.geoserver.wps.gs.resource.model.Dimension;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.impl.VectorialLayer;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;
import org.geoserver.wps.gs.resource.model.translate.TranslateItemUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;

/**
 * Implementation of the {@link TranslateItem} for the management of a GeoServer {@link DataStore}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class DataStoreItem extends TranslateItem {

    private Map<String, String> store;

    /**
     * @return the store
     */
    public Map<String, String> getStore() {
        return store;
    }

    /**
     * @param store the store to set
     */
    public void setStore(Map<String, String> store) {
        this.store = store;
    }

    @Override
    protected TranslateItem execute(TranslateContext context) throws IOException {
        // Create the ImportData accordingly to this translate task
        ImportData data = null;
        try {
            data = convertToImprtData(context);
        } catch (Exception cause) {
            // throw new IOException("Could not convert to Importer Data", cause);
            LOGGER.log(Level.WARNING, "Could not convert to Importer Data", cause);
        }

        // Populate the Import Context
        ImportTask task = null;
        if (getOrder() == 0) {
            context.setImportContext(context.getImporter().createContext(data));
            task = context.getImportContext().getTasks().get(0);
            setLayerInfo(context.getOriginator(), context.getCatalog(), task);
        } else {
            task = context.getImportContext().getTasks().get(0);
            task.setDirect(false);

            final StoreInfo targetStore = getDataStore(context.getOriginator(),
                    context.getCatalog(), task);
            if (targetStore != null) {
                task.setStore(targetStore);
                NamespaceInfo namespace = context.getCatalog().getNamespaceByPrefix(
                        targetStore.getWorkspace().getName());
                if (namespace == null) {
                    namespace = context.getCatalog().getDefaultNamespace();
                }
                task.getLayer().getResource().setNamespace(namespace);
                context.getImportContext().setTargetWorkspace(targetStore.getWorkspace());
                context.getImportContext().setTargetStore(targetStore);
            }
        }

        return null;
    }

    /**
     * 
     * @param context
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private ImportData convertToImprtData(TranslateContext context) throws URISyntaxException,
            IOException {
        if (this.store.containsKey("url") && this.store.get("url").startsWith("file:")) {
            // CSV or Shape
            final File file = TranslateItemUtils.getFileFromUrl(context, this.store);
            DataFormat dataFormat = DataFormat.lookup(file);
            CSVDataStoreFactory csvDataStoreFactory = new CSVDataStoreFactory();
            if ((dataFormat.getName().equals("CSV") && csvDataStoreFactory.canProcess(DataUtilities
                    .fileToURL(file))) || dataFormat.getName().equals("Shapefile")) {
                SpatialFile spatialData = new SpatialFile(file);
                spatialData.prepare();
                return spatialData;
            }
        } else if (this.store.containsKey("dbtype") || this.store.containsKey("database")) {
            Map<String, Serializable> params = new HashMap<String, Serializable>();
            params.putAll(store);
            return new Database(params);
        }

        return null;
    }

    /**
     * 
     * @param resource
     * @param catalog
     * @param task
     * @return
     * @throws MalformedURLException
     */
    private StoreInfo getDataStore(Resource resource, Catalog catalog, ImportTask task)
            throws MalformedURLException {
        VectorialLayer userLayer = (VectorialLayer) resource;
        DataStoreInfo dataStore = catalog.getDataStoreByName(resource.getName());
        if (dataStore != null) {
            task.setUpdateMode(UpdateMode.REPLACE);
            return dataStore;
        }

        dataStore = catalog.getFactory().createDataStore();
        dataStore.setName(resource.getName());
        WorkspaceInfo workspace = null;
        if (userLayer.getWorkspace() != null) {
            for (WorkspaceInfo wk : catalog.getWorkspaces()) {
                if (wk.getName().equalsIgnoreCase(userLayer.getWorkspace())) {
                    workspace = wk;
                }
            }
        } else {
            // the DEFAULT one
            workspace = catalog.getDefaultWorkspace();
        }
        dataStore.setWorkspace(workspace);
        dataStore.getConnectionParameters().putAll(this.store);
        dataStore.setEnabled(true);
        catalog.add(dataStore);

        task.setUpdateMode(UpdateMode.CREATE);

        return dataStore;
    }

    /**
     * 
     * @param originator
     * @param catalog
     * @param task
     * @throws IOException
     */
    private void setLayerInfo(Resource originator, Catalog catalog, ImportTask task)
            throws IOException {
        LayerInfo layer = task.getLayer();

        // check if the layer exists
        if (catalog.getLayerByName(layer.getName()) != null) {
            catalog.remove(layer.getResource());
            catalog.remove(layer);
        }

        // set the correct Resource information
        VectorialLayer userLayer = (VectorialLayer) originator;
        ResourceInfo resource = layer.getResource();
        if (userLayer.nativeBoundingBox() != null)
            resource.setNativeBoundingBox(userLayer.nativeBoundingBox());
        if (userLayer.latLonBoundingBox() != null)
            resource.setLatLonBoundingBox(userLayer.latLonBoundingBox());
        if (userLayer.nativeCRS() != null)
            resource.setNativeCRS(userLayer.nativeCRS());
        if (userLayer.getSrs() != null)
            resource.setSRS(userLayer.getSrs());

        layer.setName(userLayer.getName());
        layer.setAbstract(userLayer.getAbstract());
        layer.setTitle(userLayer.getTitle());

        if (originator.getDimensions() != null) {
            for (Dimension dim : originator.getDimensions()) {
                resource.getMetadata().put(dim.getName(), dim.getDimensionInfo());
            }
        }

        StyleInfo defaultStyle = userLayer.defaultStyle(catalog);
        if (defaultStyle != null) {
            layer.setDefaultStyle(defaultStyle);
        }

        task.getData().prepare();
    }

}
