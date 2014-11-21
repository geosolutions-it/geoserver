{
  "map": {
    "center": [
      7686755.1552848, 
      3528761.9601233
    ], 
    "layers": [
      <#list layers as layer>
		  {
			"format":${layer.format}, 
			"name":${layer.name}, 
			"opacity":${layer.opacity}, 
			"selected":${layer.selected}, 
			"source":${layer.source}, 
			"styles":${layer.styles}, 
			"title":${layer.title}, 
			"transparent":${layer.transparent}, 
			"visibility":${layer.visibility},
			"queryable":${layer.queryable},
			"dimensions":{
				"time":{
					"name":${layer.timeDimensions.name},
					"units":${layer.timeDimensions.units},
					"unitsymbol":${layer.timeDimensions.unitsymbol},
					"nearestVal":${layer.timeDimensions.nearestVal},
					"multipleVal":${layer.timeDimensions.multipleVal},
					"current":${layer.timeDimensions.current},
					"default":${layer.timeDimensions.defaultVal},
					"values":["${layer.timeDimensions.minLimit}","${layer.timeDimensions.maxLimit}"]
				}
			},
		   "llbbox": [${layer.timeDimensions.minX},${layer.timeDimensions.minY},${layer.timeDimensions.maxX},${layer.timeDimensions.maxY}],
		   "tileSets": [{"srs":{"EPSG:900913":true},"bbox":{"EPSG:900913":{"bbox":[939258.2034374997,5322463.1528125,1252344.2712499984,5635549.220624998],"srs":"EPSG:900913"}},"resolutions":[4891.9698095703125,2445.9849047851562,1222.9924523925781,611.4962261962891,305.74811309814453,152.87405654907226,76.43702827453613,38.218514137268066,19.109257068634033,9.554628534317017,4.777314267158508,2.388657133579254,1.194328566789627,0.5971642833948135,0.29858214169740677],"width":256,"height":256,"format":"image/png8","layers":"ndvi:ndvi","styles":""}]
		  }
	  </#list>

    ], 
    "maxExtent": [
      -20037508.34, 
      -20037508.34, 
      20037508.34, 
      20037508.34
    ], 
	"extent": [
      -???, 
      -???, 
      20037508.34, 
      20037508.34
    ], 
    "projection": "EPSG:900913", 
    "units": "m", 
    "zoom": 6
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
			"title": "Acque GeoServer",
			"projection":"EPSG:4326",
			"url": "http://84.33.2.75/geoserver/ows",
			"version":"1.1.1",
			"layersCachedExtent":[-2.003750834E7,-2.003750834E7,2.003750834E7,2.003750834E7],
                        "useCapabilities":false,
			"layerBaseParams": {
					"TILED": true,
                    "FORMAT":"image/png8"
                          }
		}, 
     
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
