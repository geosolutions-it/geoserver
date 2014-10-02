/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.DataFormat;
import org.geoserver.importer.Database;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.SpatialFile;
import org.geoserver.importer.UpdateMode;
import org.geoserver.importer.csv.CSVDataStoreFactory;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.impl.VectorialLayer;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;

/**
 * 
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
        ImportData data;
        try {
            data = convertToImprtData();
        } catch (URISyntaxException cause) {
            throw new IOException("Could not convert to Importer Data", cause);
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
                context.getImportContext().setTargetStore(targetStore);
            }
        }

        return null;
    }

    /**
     * 
     * @return
     * @throws URISyntaxException
     * @throws IOException
     */
    private ImportData convertToImprtData() throws URISyntaxException, IOException {
        if (this.store.containsKey("url") && this.store.get("url").startsWith("file")) {
            // CSV or Shape
            final URL url = new URL(this.store.get("url"));
            final File file = new File(url.toURI());
            DataFormat dataFormat = DataFormat.lookup(file);
            CSVDataStoreFactory csvDataStoreFactory = new CSVDataStoreFactory();
            if ((dataFormat.getName().equals("CSV") && csvDataStoreFactory.canProcess(url))
                    || dataFormat.getName().equals("Shapefile")) {
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
     */
    private StoreInfo getDataStore(Resource resource, Catalog catalog, ImportTask task) {
        if (this.store.containsKey("dbtype") || this.store.containsKey("database")) {
            DataStoreInfo dataStore = catalog.getDataStoreByName(resource.getName());
            if (dataStore != null) {
                task.setUpdateMode(UpdateMode.REPLACE);
                return dataStore;
            }
            
            dataStore = catalog.getFactory().createDataStore();
            dataStore.setName(resource.getName());
            dataStore.setWorkspace(catalog.getDefaultWorkspace());
            dataStore.getConnectionParameters().putAll(this.store);
            dataStore.setEnabled(true);
            catalog.add(dataStore);
            task.setUpdateMode(UpdateMode.CREATE);
            return dataStore;
        }

        return null;
    }

    /**
     * 
     * @param originator
     * @param catalog
     * @param task
     */
    private void setLayerInfo(Resource originator, Catalog catalog, ImportTask task) {
        LayerInfo layer = task.getLayer();
        
        // check if the layer exists
        if (catalog.getLayerByName(layer.getName()) != null ) {
            catalog.remove(layer.getResource());
            catalog.remove(layer);
        }
        
        // set the correct Resource information
        VectorialLayer userLayer = (VectorialLayer) originator;
        ResourceInfo resource = layer.getResource();
        resource.setSRS(userLayer.getSrs());
//        resource.setNativeBoundingBox(box);
//        resource.setNativeCRS(nativeCRS);
        
        layer.setName(userLayer.getName());
        layer.setAbstract(userLayer.getAbstract());
        layer.setTitle(userLayer.getTitle());
//        layer.setDefaultStyle(defaultStyle);
    }

}
