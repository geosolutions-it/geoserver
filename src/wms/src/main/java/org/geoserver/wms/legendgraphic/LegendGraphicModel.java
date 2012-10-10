package org.geoserver.wms.legendgraphic;

import org.geoserver.wms.GetLegendGraphicRequest;

public class LegendGraphicModel {

    private final GetLegendGraphicRequest request;
    
    public LegendGraphicModel(GetLegendGraphicRequest r){
        super();
        this.request=r;
    }

    public GetLegendGraphicRequest getRequest() {
        return request;
    }
    

}
