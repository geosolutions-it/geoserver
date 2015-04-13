/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.resource.model.translate.impl;

import java.io.IOException;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.wps.gs.resource.model.Dimension;
import org.geoserver.wps.gs.resource.model.Resource;
import org.geoserver.wps.gs.resource.model.impl.VectorialLayer;
import org.geoserver.wps.gs.resource.model.translate.TranslateContext;
import org.geoserver.wps.gs.resource.model.translate.TranslateItem;
import org.geotools.data.DataAccess;
import org.geotools.geometry.jts.Geometries;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.VirtualTable;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Implementation of the {@link TranslateItem} for the management of a GeoServer {@link VirtualTable}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 * 
 */
public class VirtualTableItem extends TranslateItem {

    private Map<String, String> metadata;

    /**
     * @return the metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }

    /**
     * @param metadata the metadata to set
     */
    public void setMetadata(Map<String, String> metadata) {
        this.metadata = metadata;
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    protected TranslateItem execute(TranslateContext context) throws IOException {
        final Catalog catalog = context.getCatalog();
        final CatalogBuilder builder = new CatalogBuilder(catalog);

        if (catalog != null) {
            DataStoreInfo dataStore = catalog.getDataStoreByName(metadata.get("dataStore"));

            if (dataStore != null) {
                final Resource originator = context.getOriginator();
                VectorialLayer userLayer = (VectorialLayer) originator;

                builder.setStore(dataStore);
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
                
                builder.setWorkspace(workspace);
                
                DataAccess<? extends FeatureType, ? extends Feature> dataAccess = dataStore.getDataStore(null);
                
                if (dataAccess instanceof JDBCDataStore) {
                    JDBCDataStore jstore = (JDBCDataStore) dataAccess;
                    
                    // set Virtual Table metadata and save to the catalog
                    VirtualTable virtualTable = new VirtualTable(metadata.get("name"), metadata.get("sql"), Boolean.valueOf(metadata.get("escapeSql")));
                    Geometries geomType = Geometries.getForName(metadata.get("geometryType"));
                    Class binding = geomType == null ? Geometry.class : geomType.getBinding();
                    virtualTable.addGeometryMetadatata(metadata.get("geometryName"), binding, Integer.valueOf(metadata.get("geometrySrid")));
                    
                    jstore.createVirtualTable(virtualTable);
                    //jstore.addVirtualTable(virtualTable);

                    builder.setStore(dataStore);
                    FeatureTypeInfo fti = builder.buildFeatureType(jstore.getFeatureSource(virtualTable.getName()));
                    fti.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, virtualTable);
                    LayerInfo layerInfo = builder.buildLayer(fti);
                    
                    // Set Resource and Layer Info
                    ResourceInfo resource = layerInfo.getResource();
                    if (userLayer.nativeBoundingBox() != null)
                        resource.setNativeBoundingBox(userLayer.nativeBoundingBox());
                    if (userLayer.latLonBoundingBox() != null)
                        resource.setLatLonBoundingBox(userLayer.latLonBoundingBox());
                    if (userLayer.nativeCRS() != null)
                        resource.setNativeCRS(userLayer.nativeCRS());
                    if (userLayer.getSrs() != null)
                        resource.setSRS(userLayer.getSrs());
                    NamespaceInfo nameSpace = catalog.getNamespaceByPrefix(workspace.getName());
                    if (nameSpace != null)
                        resource.setNamespace(nameSpace);
//                    resource.setStore(dataStore);
//                    resource.setName(userLayer.getName());
//                    resource.setNativeName(userLayer.getName());
                    
                    layerInfo.setName(userLayer.getName());
                    layerInfo.setAbstract(userLayer.getAbstract());
                    layerInfo.setTitle(userLayer.getTitle());

                    if (originator.getDimensions() != null) {
                        for (Dimension dim : originator.getDimensions()) {
                            resource.getMetadata().put(dim.getName(), dim.getDimensionInfo());
                        }
                    }

                    StyleInfo defaultStyle = userLayer.defaultStyle(catalog);
                    if (defaultStyle != null) {
                        layerInfo.setDefaultStyle(defaultStyle);
                    }
                    
                    // Add to the Catalog
                    catalog.add(fti);
                    catalog.add(layerInfo);
                }
            }
        }

        return null;
    }

}
