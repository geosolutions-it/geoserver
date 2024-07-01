Templates With FreeMarker
-------------------------

MapML templates are written in `Freemarker <http://www.freemarker.org/>`_ , a Java-based template engine. The templates below are feature type specific and will not be applied in multi-layer WMS requests.  See :ref:`tutorial_freemarkertemplate` for general information about FreeMarker implementation in GeoServer.

GetMap MapML HTML Preview/Layer Preview Head Stylesheet Templating
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
The viewer is returned when the format includes subtype=mapml. The viewer is an HTML document that includes a head section with a link to the stylesheet. The default viewer is a simple viewer that includes a link to the default stylesheet. 
A template can be created to insert links to whole stylesheet or actual stylesheet elements.  
We can do this by creating a file called ``mapml-head-viewer.ftl`` in the GeoServer data directory in the directory for the layer that we wish to append links to.  For example we could create this file under ``workspaces/topp/states_shapefile/states``.  To add stylesheet links and stylesheet elements, we enter the following text inside this new file:

.. code-block:: html

 <!-- Added from the template -->	
 <link rel="stylesheet" href="mystyle.css">
 <style>
  body {
   background-color: linen;
  }
 </style>
 <!-- End of added from the template -->

This would result in a head section that would resemble:

.. code-block:: html

    <head>
      <title>World Map</title>
      <meta charset='utf-8'>
      <script type="module"  src="http://localhost:8080/geoserver/mapml/viewer/widget/mapml-viewer.js"></script>
      <style>
          html, body { height: 100%; }
          * { margin: 0; padding: 0; }
          mapml-viewer:defined { max-width: 100%; width: 100%; height: 100%; border: none; vertical-align: middle }
          mapml-viewer:not(:defined) > * { display: none; } nlayer- { display: none; }
      </style>
      <noscript>
      <style>
          mapml-viewer:not(:defined) > :not(layer-) { display: initial; }
      </style>
      </noscript>
      <!-- Added from the template -->
      <link rel="stylesheet" href="mystyle.css">
      <style>
          body {
              background-color: linen;
          }
      </style>
      <!-- End of added from the template -->
    </head>

GetMap XML Head Stylesheet Templating
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
MapML in XML format includes a map-head element that includes map-link elements to link to other resources, including map style variants.  Additional map-link elements can be added to the map-head element by creating a mapml-head.ftl template in the GeoServer data directory in the directory for the layer we wish to append map-links to.  For example we could create the head.ftl file under ``workspaces/tiger/poly_landmarks_shapefile/poly_landmarks``: 

.. code-block:: bash

 <!-- Added from the template -->	
 <map-style>.polygon-r1-s1{stroke-opacity:3.0; stroke-dashoffset:4; stroke-width:2.0; fill:#AAAAAA; fill-opacity:3.0; stroke:#DD0000; stroke-linecap:butt}</map-style>
 <map-link href="${serviceLink("${base}","${path}","${kvp}")}" rel="\${rel}" title="templateinsertedstyle"/>
 <!-- End of added from the template -->

This would result in a map-head section that would resemble:

.. code-block:: html

    <map-head>
      <map-title>Manhattan (NY) landmarks</map-title>
      <map-base href="http://localhost:8080/geoserver/wms"/>
      <map-meta charset="utf-8"/>
      <map-meta content="text/mapml;projection=WGS84" http-equiv="Content-Type"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=grass&amp;version=1.3.0&amp;height=384" rel="style" title="grass"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=restricted&amp;version=1.3.0&amp;height=384" rel="style" title="restricted"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=polygon%2C&amp;version=1.3.0&amp;height=384" rel="self style" title="polygon,"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AOSMTILE&amp;service=WMS&amp;bbox=-2.0037508342789244E7%2C-2.364438881673656E7%2C2.0037508342789244E7%2C2.364438881673657E7&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;version=1.3.0&amp;height=384" rel="alternate" projection="OSMTILE"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3ACBMTILE&amp;service=WMS&amp;bbox=-8079209.971443829%2C-3626624.322362231%2C8281691.192343056%2C1.233598344760506E7&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;version=1.3.0&amp;height=384" rel="alternate" projection="CBMTILE"/>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AAPSTILE&amp;service=WMS&amp;bbox=-1.06373184982574E7%2C-1.06373184982574E7%2C1.46373184982574E7%2C1.46373184982574E7&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;version=1.3.0&amp;height=384" rel="alternate" projection="APSTILE"/>
      <map-style>.bbox {display:none} .polygon-r1-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:1.0; fill:#AAAAAA; fill-opacity:1.0; stroke:#000000; stroke-linecap:butt}</map-style>
      <!-- Added from the template -->	
      <map-style>.polygon-r2-s2{stroke-opacity:3.0; stroke-dashoffset:4; stroke-width:2.0; fill:#AAAAAA; fill-opacity:3.0; stroke:#DD0000; stroke-linecap:butt}</map-style>
      <map-link href="http://localhost:8080/geoserver/tiger/wms?format_options=mapml-wms-format%3Aimage%2Fpng&amp;request=GetMap&amp;crs=MapML%3AWGS84&amp;service=WMS&amp;bbox=-180.0%2C-90.0%2C180.0%2C90.0&amp;format=text%2Fmapml&amp;layers=poly_landmarks&amp;width=768&amp;styles=templateinsertedstyle&amp;version=1.3.0&amp;height=384" rel="style" title="templateinsertedstyle"/>
      <!-- End of added from the template -->
    </map-head>

GetMap Features Inline Style Class Templating
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
MapML in XML feature format (when the parameter format_options=mapmlfeatures:true is set) has a map-head element that includes map-style elements where the style classes are defined.  
Within the map-body, map-feature elements include map-geometry with map-coordinates.    

The mapml-feature-head.ftl is a file that can be used to insert map-style elements with the style class definitions.
This file is placed in the GeoServer data directory in the directory for the layer we wish to append style classes to.  For example we could create the file under ``workspaces/tiger/poly_landmarks_shapefile/poly_landmarks``.  

The mapml-feature-head.ftl file would look like::

      <mapml- xmlns="http://www.w3.org/1999/xhtml">
        <map-head>
          <map-style>.desired {stroke-dashoffset:3}</map-style>
        </map-head>
      </mapml->  

This would result in a MapML feature output header that would resemble:

.. code-block:: xml

    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
        <map-title>poi</map-title>
        <map-meta charset="UTF-8"/>
        <map-meta content="text/mapml" http-equiv="Content-Type"/>
        <map-meta name="extent" content="top-left-longitude=-74.011832,top-left-latitude=40.711946,bottom-right-longitude=-74.008573,bottom-right-latitude=40.707547"/>
        <map-meta name="cs" content="gcrs"/>
        <map-meta name="projection" content="MapML:EPSG:4326"/>
        <map-style>.bbox {display:none} .polygon-r1-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:1.0; fill:#AAAAAA; fill-opacity:1.0; stroke:#000000; stroke-linecap:butt}</map-style>
        <map-style>.desired {stroke-dashoffset:3}</map-style>
      </map-head>
    </mapml->

The mapml-feature.ftl is a file can be used to insert map-style elements with the style class definitions into the map-head.  Note that this section of the template adds the styles listed but does not remove any existing styles.
It can be used to edit map-property names and values in a manner similar to :ref:`tutorials_getfeatureinfo_geojson`.  It also can be used to add style class identifiers to map-feature elements based on the feature identifier or to wrap groupings of map-coordinates with spans that specify the style class based on an index of coordinate order (zero based index that starts at the first coordinate pair of each feature).  
This file is placed in the GeoServer data directory in the directory for the layer we wish to append style classes to.  For example we could create the file under ``workspaces/tiger/poly_landmarks_shapefile/poly_landmarks``.  

An example mapml-feature.ftl file to modify a point layer would look like::
    
    <mapml- xmlns="http://www.w3.org/1999/xhtml">
      <map-head>
      </map-head>
      <map-body>
        <map-feature>
          <#list attributes as attribute>
            <#if attribute.name == "NAME">
              <map-properties name="UPDATED ${attribute.name}" value="CHANGED ${attribute.value}"/>
            <#else>
              <map-properties name="${attribute.name}" value="${attribute.value}"/>
            </#if>
          </#list>
          <#list attributes as gattribute>
            <#if gattribute.isGeometry>
              <map-geometry>
                <map-point>
                  <map-coordinates><#list gattribute.rawValue.coordinates as coord>
                    <#if coord?index == 0><![CDATA[<span class="desired">]]>${coord.x} ${coord.y}<![CDATA[</span>]]><#else>${coord.x} ${coord.y}</#if></#list></map-coordinates></map-point>
                </map-geometry>
             </#if>
           </#list>
          </map-feature>
        </map-body>
        </mapml->

This would result in a MapML feature output body that would resemble:
    
    <mapml- xmlns="http://www.w3.org/1999/xhtml"><map-head><map-title>Bridges</map-title><map-meta charset="UTF-8"/><map-meta content="text/mapml" http-equiv="Content-Type"/><map-meta name="extent" content="top-left-longitude=-180.000000,top-left-latitude=90.000000,bottom-right-longitude=180.000000,bottom-right-latitude=-90.000000"/><map-meta name="cs" content="gcrs"/><map-meta name="projection" content="MapML:urn:ogc:def:crs:EPSG::4326"/><map-style>.bbox {display:none} .Bridges-r1-s1{r:48.0; stroke-opacity:1.0; stroke-dashoffset:0; well-known-name:square; stroke-width:1.0; opacity:1.0; fill:#808080; fill-opacity:1.0; stroke:#000000; stroke-linecap:butt}</map-style></map-head><map-body><map-feature id="Bridges.1107531599613" class="Bridges-r1-s1"><map-geometry><map-point><map-coordinates>&lt;span class="desired">0.0007 0.0002&lt;/span></map-coordinates></map-point></map-geometry><map-properties><table xmlns="http://www.w3.org/1999/xhtml"><thead><tr><th role="columnheader" scope="col">Property name</th><th role="columnheader" scope="col">Property value</th></tr></thead><tbody><tr><th scope="row">FID</th><td itemprop="FID">110</td></tr><tr><th scope="row">NAME</th><td itemprop="NAME">Cam Bridge</td></tr></tbody></table></map-properties></map-feature></map-body></mapml->

Note that in addition to tagging the coordinates with a style class, the template also changes the name of the NAME property to "UPDATED NAME" and the value to "CHANGED Cam Bridge".  

A more complex example mapml-feature.ftl that operates on a multi-polygon layer and only inserts span tags into a specific feature::

    <mapml- xmlns=\"http://www.w3.org/1999/xhtml\">
        <map-head>
        </map-head>
        <map-body>
        <map-feature>
        <#if attributes.FID.value == \"117">
          <#list attributes as attribute>
            <#if attribute.isGeometry>
              <map-geometry>
                <map-multipolygon>
              <#list 0 ..< attribute.rawValue.getNumGeometries() as index>
                <#assign polygon = attribute.rawValue.getGeometryN(index)>
               <map-polygon>
               <#assign shell = polygon.getExteriorRing()><map-coordinates><#list shell.coordinates as coord><#if coord?index == 0><![CDATA[<span class="desired">]]>${coord.x} ${coord.y}<#elseif coord?index == 4> ${coord.x} ${coord.y}<![CDATA[</span>]]><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates>
              <#list 0 ..< polygon.getNumInteriorRing() as index>
                <#assign hole = polygon.getInteriorRingN(index)><map-coordinates><#list hole.coordinates as coord><#if coord?index == 0><![CDATA[<span class="desired">]]>${coord.x} ${coord.y} <#elseif coord?index == 4> ${coord.x} ${coord.y}<![CDATA[</span>]]><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates></#list>
               </map-polygon>
                </#list>
               </map-multipolygon>
              </map-geometry>
            </#if>
          </#list>
        <#else>
          <#list attributes as attribute>
            <#if attribute.isGeometry>
              <map-geometry>
                <map-multipolygon>
              <#list 0 ..< attribute.rawValue.getNumGeometries() as index>
                <#assign polygon = attribute.rawValue.getGeometryN(index)>
               <map-polygon>
               <#assign shell = polygon.getExteriorRing()><map-coordinates><#list shell.coordinates as coord> ${coord.x} ${coord.y} </#list></map-coordinates>
              <#list 0 ..< polygon.getNumInteriorRing() as index>
                <#assign hole = polygon.getInteriorRingN(index)><map-coordinates><#list hole.coordinates as coord> ${coord.x} ${coord.y} </#list></map-coordinates></#list>
               </map-polygon>
                </#list>
               </map-multipolygon>
              </map-geometry>
            </#if>
          </#list>
        </#if>
        </map-feature>
        </map-body>
        </mapml- >