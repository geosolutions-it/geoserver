<#setting number_format="0.##">
 {
  "map": {
    "center": [
      ${map.centerX}, 
      ${map.centerY}
    ], 
    "layers": [
      {
        "fixed": true, 
        "group": "background", 
        "name": "ROADMAP", 
        "opacity": 1, 
        "selected": false, 
        "source": "google", 
        "title": "Google Roadmap", 
        "visibility": false
      }, 
      {
        "fixed": true, 
        "group": "background", 
        "name": "TERRAIN", 
        "opacity": 1, 
        "selected": false, 
        "source": "google", 
        "title": "Google Terrain", 
        "visibility": false
      }, 
      {
        "fixed": true, 
        "group": "background", 
        "name": "HYBRID", 
        "opacity": 1, 
        "selected": false, 
        "source": "google", 
        "title": "Google Hybrid", 
        "visibility": false
      }, 
      {
        "fixed": true, 
        "group": "background", 
        "name": "osm", 
        "opacity": 1, 
        "selected": false, 
        "source": "mapquest", 
        "title": "MapQuest OpenStreetMap", 
        "visibility": false
      }, 
      {
        "fixed": true, 
        "group": "background", 
        "name": "mapnik", 
        "opacity": 1, 
        "selected": false, 
        "source": "osm", 
        "title": "Open Street Map", 
        "visibility": false
      }, 
      {
        "fixed": true, 
        "group": "background", 
        "name": "Aerial", 
        "opacity": 1, 
        "selected": false, 
        "source": "bing", 
        "title": "Bing Aerial", 
        "visibility": false
      }, 
      {
        "fixed": true, 
        "group": "background", 
        "name": "AerialWithLabels", 
        "opacity": 1, 
        "selected": false, 
        "source": "bing", 
        "title": "Bing Aerial With Labels", 
        "visibility": true
      }, 
      {
        "args": [
          "None", 
          {
            "visibility": false
          }
        ], 
        "fixed": true, 
        "group": "background", 
        "opacity": 1, 
        "selected": false, 
        "source": "ol", 
        "title": "None", 
        "type": "OpenLayers.Layer", 
        "visibility": false
      }, 
      <#list map.layers as layer>
      {
        "format": "${layer.format}", 
        "name": "${layer.name}", 
        "opacity": ${layer.opacity}, 
        "selected": ${layer.selected}, 
        "source": "${layer.source}", 
        "styles": "${layer.styles}", 
        "title": "${layer.title}", 
        "transparent": ${layer.transparent}, 
        "visibility": ${layer.visibility},
        "queryable":${layer.queryable},
        "dimensions":{
            "time":{
                "name":"${layer.timeDimensions.name}",
                "units":"${layer.timeDimensions.units}",
                "unitsymbol":"${layer.timeDimensions.unitsymbol}",
                "nearestVal":${layer.timeDimensions.nearestVal},
                "multipleVal":${layer.timeDimensions.multipleVal},
                "current":${layer.timeDimensions.current},
                "default":"${layer.timeDimensions.defaultVal}",
                "values":["${layer.timeDimensions.minLimit}","${layer.timeDimensions.maxLimit}"]
          	}
        },
       "llbbox": [${layer.timeDimensions.minX},${layer.timeDimensions.minY},${layer.timeDimensions.maxX},${layer.timeDimensions.maxY}]
      }<#if layer_has_next>,</#if>
      </#list>
      <#list map.metocs as metoc>
      {
        "format": "image/png", 
        "name": "${metoc.owsResourceIdentifier}", 
        "opacity": 1, 
        "selected": false, 
        "source": "${metoc.sourceId}_${(metoc_index+1)}", 
        "styles": "", 
        "title": "${metoc.title}", 
        "transparent": true, 
        "visibility": true,
        "queryable": true,
        "dimensions":{
            "time":{
                "name":"time",
                "units":"ISO8601",
                "unitsymbol":"",
                "nearestVal":false,
                "multipleVal":false,
                "current":false,
                "default":""
          	}
        }
      }<#if metoc_has_next>,</#if>
      </#list>
      <#list map.rawData as raw>
      ,{"${raw.name}" : "${raw.text}"}
      </#list>
    ], 
    "maxExtent": [
      ${map.maxExtentMinX}, 
      ${map.maxExtentMinY}, 
      ${map.maxExtentMaxX}, 
      ${map.maxExtentMaxY}
    ], 
    "extent": [
      ${map.extentMinX}, 
      ${map.extentMinY}, 
      ${map.extentMaxX}, 
      ${map.extentMaxY}
    ], 
    "projection": "${map.projection}", 
    "units": "${map.units}", 
    "zoom": ${map.zoom}
  }, 
  "sources": {
    "bing": {
      "projection": "EPSG:900913", 
      "ptype": "gxp_bingsource"
    }, 
    "google": {
      "projection": "EPSG:900913", 
      "ptype": "gxp_googlesource"
    }, 
    "geoserver": {
        "ptype": "gxp_wmssource",
        "title": "GeoServer",
        "projection":"EPSG:4326",
        "url": "http://localhost:8080/geoserver/ows",
        "version":"1.1.1",
        "layersCachedExtent":[-2.003750834E7,-2.003750834E7,2.003750834E7,2.003750834E7],
                        "useCapabilities":false,
        "layerBaseParams": {
            "TILED": true,
            "FORMAT":"image/png8"
        }
    }, 
    <#list map.metocs as metoc>
    "${metoc.sourceId}_${(metoc_index+1)}": {
        "ptype": "gxp_wmssource",
        "title": "${metoc.title}",
        "projection":"EPSG:4326",
        "url": "${metoc.owsBaseURL}",
        "version":"${metoc.owsVersion}",
        "layersCachedExtent":[-2.003750834E7,-2.003750834E7,2.003750834E7,2.003750834E7],
                        "useCapabilities":false,
        "layerBaseParams": {
            "TILED": true,
            "FORMAT":"image/png"
        }
    }<#if metoc_has_next>,</#if>
    </#list>,
    "mapquest": {
      "projection": "EPSG:900913", 
      "ptype": "gxp_mapquestsource"
    }, 
    "ol": {
      "projection": "EPSG:900913", 
      "ptype": "gxp_olsource"
    }, 
    "osm": {
      "projection": "EPSG:900913", 
      "ptype": "gxp_osmsource"
    }
  }
}