package org.geoserver.mapml.tcrs;

import java.util.ArrayList;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.GeoServerInitializer;

public class TiledCRSListInitializer implements GeoServerInitializer {
    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        GeoServerInfo global = geoServer.getGlobal();
        MetadataMap metadata = global.getSettings().getMetadata();
        if (!metadata.containsKey(TiledCRSConstants.TCRS_METADATA_KEY)) {
            ArrayList<String> initList = new ArrayList<>();
            metadata.put(TiledCRSConstants.TCRS_METADATA_KEY, initList);
            geoServer.save(global);
        }
    }
}
