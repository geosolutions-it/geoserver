package org.geoserver.eumetsat.pinning;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.config.impl.GeoServerLifecycleHandler;
import org.geoserver.eumetsat.pinning.rest.PinningServiceController;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.coverage.grid.io.DimensionDescriptor;
import org.geotools.coverage.grid.io.GranuleSource;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.gce.imagemosaic.catalog.GranuleCatalogSource;
import org.geotools.util.logging.Logging;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.feature.simple.SimpleFeatureType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@DependsOn("catalog")
public class LayersMapper implements GeoServerLifecycleHandler {

    private static final String CSV_FILE_NAME = "pinning/layers_mapping.csv"; // The name of the CSV file

    private static final Logger LOGGER = Logging.getLogger(LayersMapper.class);

    @Autowired
    private Catalog catalog;

    private Map<String, MappedLayer> layerMapping;

    public LayersMapper() {
        this.layerMapping = new HashMap<>();
    }

    public Map<String, MappedLayer> getLayers() {
        return layerMapping;
    }

    @PostConstruct
    private void loadMappings() {
        LOGGER.info("Loading layers mapping");
        layerMapping.clear();

        // Get the GeoServer data directory path
        GeoServerResourceLoader loader = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        String dataDirPath = loader.getBaseDirectory().getPath();

        // Set the folder where your CSV file is located
        File csvFile = new File(dataDirPath, CSV_FILE_NAME);

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            while ((line = reader.readLine()) != null) {

                if (line.trim().isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Split line by comma
                String[] parts = line.split(",");
                if (parts.length == 3) {
                    String layerId = parts[0].trim();
                    String workspace = parts[1].trim(); // Workspace, like ws1
                    String layerName = parts[2].trim(); // Layer Name, like layer11
                    MappedLayer layer = new MappedLayer(layerId, workspace, layerName);
                    initializeLayer(layer);

                    // Store the mapping
                    layerMapping.put(layerId, layer);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initializeLayer(MappedLayer layer) throws IOException {
        String gsLayerId = layer.getGeoServerLayerIdentifier();
        LayerInfo gsLayer = catalog.getLayerByName(gsLayerId);
        if (gsLayer == null) {
            LOGGER.warning("The specified layer doesn't have any associated GeoServer layer in the catalog: " + layer);
            return;
        }
        ResourceInfo resourceInfo = gsLayer.getResource();
        if (resourceInfo instanceof FeatureTypeInfo) {
            FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) resourceInfo;
            setVectorLayer(layer, featureTypeInfo);

        } else if (resourceInfo instanceof CoverageInfo) {
            CoverageInfo cvInfo = (CoverageInfo) resourceInfo;
            setMosaicLayer(layer, cvInfo);
        }
    }

    private void setMosaicLayer(MappedLayer layer, CoverageInfo cvInfo) throws IOException {
        MetadataMap metadataMap = cvInfo.getMetadata();
        DimensionInfo timeDimension = metadataMap.get("time", DimensionInfo.class);
        if (timeDimension != null) {
            GridCoverageReader reader = cvInfo.getGridCoverageReader(null, null);
            if (reader instanceof StructuredGridCoverage2DReader) {
                StructuredGridCoverage2DReader structuredReader = (StructuredGridCoverage2DReader) reader;
                String nativeCoverageName = cvInfo.getNativeCoverageName();
                if (nativeCoverageName == null) {
                    nativeCoverageName = reader.getGridCoverageNames()[0];
                }
                GranuleSource source = structuredReader.getGranules(nativeCoverageName, true);
                SimpleFeatureType schema = source.getSchema();
                String tableName = schema.getTypeName();
                layer.setTableName(tableName);
                List<DimensionDescriptor> descriptors = structuredReader.getDimensionDescriptors(nativeCoverageName);
                for (DimensionDescriptor desc: descriptors) {
                    if ("TIME".equalsIgnoreCase(desc.getName())) {
                        String timeAttribute = desc.getStartAttribute();
                        layer.setTemporalAttribute(timeAttribute);
                    }
                }

            }
        }
    }

    private void setVectorLayer(MappedLayer layer, FeatureTypeInfo featureTypeInfo) {
        MetadataMap metadataMap = featureTypeInfo.getMetadata();
        DimensionInfo timeDimension = metadataMap.get("time", DimensionInfo.class);
        if (timeDimension != null) {
            String timeAttribute = timeDimension.getAttribute();
            if (timeAttribute != null) {
                layer.setTemporalAttribute(timeAttribute);
            }
            StoreInfo store = featureTypeInfo.getStore();
            String tableName = featureTypeInfo.getNativeName();
            Map<String, Serializable> params = store.getConnectionParameters();
            if ("Vector Mosaic Data Store".equalsIgnoreCase(store.getType())) {
                tableName = tableName.substring(0, tableName.length() - 7); // Get rid of the _mosaic suffix
                String delegateStoreName = (String) params.get("delegateStoreName");
                if (delegateStoreName != null) {
                    String[] storeName = delegateStoreName.split(":");
                    store = catalog.getDataStoreByName(storeName[0],storeName[1]);
                    params = store.getConnectionParameters();
                }
            }

            String schema = (String) params.get("schema");
            layer.setTableName(schema + "." + tableName);
        }
    }


    @Override
    public void onReset() {
        loadMappings();
    }

    @Override
    public void onDispose() {

    }

    @Override
    public void beforeReload() {
        loadMappings();
    }

    @Override
    public void onReload() {

    }
}
