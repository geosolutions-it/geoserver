<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>graticule</Name>
    <UserStyle>
      <FeatureTypeStyle>
        <Name>name</Name>
        <Rule>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:PropertyName>level</ogc:PropertyName>
              <ogc:Function name="Categorize">
                <ogc:Function name="env"><ogc:Literal>wms_scale_denominator</ogc:Literal></ogc:Function>
                <ogc:Literal>-1</ogc:Literal> <!-- do not display-->
                <ogc:Literal>3000000</ogc:Literal>
                <ogc:Literal>0</ogc:Literal>
                <ogc:Literal>5000000</ogc:Literal>
                <ogc:Literal>1</ogc:Literal>
                <ogc:Literal>10000000</ogc:Literal>
                <ogc:Literal>2</ogc:Literal>
                <ogc:Literal>30000000</ogc:Literal>
                <ogc:Literal>3</ogc:Literal>
                <ogc:Literal>50000000</ogc:Literal>
                <ogc:Literal>4</ogc:Literal>
              </ogc:Function>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <LineSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#bbbbbb</CssParameter>
              <CssParameter name="stroke-dasharray">3 3</CssParameter>
            </Stroke>
          </LineSymbolizer>
        </Rule>        
      </FeatureTypeStyle>
      <FeatureTypeStyle>
        <Name>Labels for square CRSs</Name>
        <Transformation>
          <ogc:Function name="vec:GraticuleLabelPoint">
            <ogc:Function name="parameter">
              <ogc:Literal>grid</ogc:Literal>
            </ogc:Function>
            <ogc:Function name="parameter">
              <ogc:Literal>offset</ogc:Literal>
              <ogc:Literal>4</ogc:Literal>
            </ogc:Function>
          </ogc:Function>
        </Transformation>
        <Rule>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsEqualTo>
                <ogc:Function name="in">
                  <ogc:Function name="env"><ogc:Literal>wms_srs</ogc:Literal></ogc:Function>
                  <ogc:Literal>EPSG:3995</ogc:Literal>
                  <ogc:Literal>EPSG:70201</ogc:Literal>
                  <ogc:Literal>EPSG:70203</ogc:Literal>
                  <ogc:Literal>EPSG:70205</ogc:Literal>
                  <ogc:Literal>EPSG:70207</ogc:Literal>
                  <ogc:Literal>EPSG:70209</ogc:Literal>
                  <ogc:Literal>EPSG:70211</ogc:Literal>
                </ogc:Function>
                <ogc:Literal>false</ogc:Literal>
              </ogc:PropertyIsEqualTo>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>level</ogc:PropertyName>
                <ogc:Function name="Categorize">
                  <ogc:Function name="env"><ogc:Literal>wms_scale_denominator</ogc:Literal></ogc:Function>
                  <ogc:Literal>-1</ogc:Literal> <!-- do not display-->
                  <ogc:Literal>3000000</ogc:Literal>
                  <ogc:Literal>0</ogc:Literal>
                  <ogc:Literal>5000000</ogc:Literal>
                  <ogc:Literal>1</ogc:Literal>
                  <ogc:Literal>10000000</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>
                  <ogc:Literal>30000000</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>
                  <ogc:Literal>50000000</ogc:Literal>
                  <ogc:Literal>4</ogc:Literal>
                </ogc:Function>
              </ogc:PropertyIsEqualTo>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Label>
              <ogc:PropertyName>label</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Noto Sans</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX><ogc:PropertyName>anchorX</ogc:PropertyName></AnchorPointX>
                  <AnchorPointY><ogc:PropertyName>anchorY</ogc:PropertyName></AnchorPointY>
                </AnchorPoint>
                <Displacement>
                  <DisplacementX><ogc:PropertyName>offsetX</ogc:PropertyName></DisplacementX>
                  <DisplacementY><ogc:PropertyName>offsetY</ogc:PropertyName></DisplacementY>
                </Displacement>
              </PointPlacement>
            </LabelPlacement>
            <Halo>
              <Radius>1</Radius>
              <Fill>
                <CssParameter name="fill">#333333</CssParameter>
                <CssParameter name="fill-opacity">0.5</CssParameter>
              </Fill>
            </Halo>
            <Fill>
              <CssParameter name="fill">#dddddd</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>      
      </FeatureTypeStyle>
      <FeatureTypeStyle>
        <Name>Labels for other CRSs</Name>
        <Rule>
          <ogc:Filter>
            <ogc:And>
              <ogc:PropertyIsEqualTo>
                <ogc:Function name="in">
                  <ogc:Function name="env"><ogc:Literal>wms_srs</ogc:Literal></ogc:Function>
                  <ogc:Literal>EPSG:3995</ogc:Literal>
                  <ogc:Literal>EPSG:70201</ogc:Literal>
                  <ogc:Literal>EPSG:70203</ogc:Literal>
                  <ogc:Literal>EPSG:70205</ogc:Literal>
                  <ogc:Literal>EPSG:70207</ogc:Literal>
                  <ogc:Literal>EPSG:70209</ogc:Literal>
                  <ogc:Literal>EPSG:70211</ogc:Literal>
                </ogc:Function>
                <ogc:Literal>true</ogc:Literal>
              </ogc:PropertyIsEqualTo>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>level</ogc:PropertyName>
                <ogc:Function name="Categorize">
                  <ogc:Function name="env"><ogc:Literal>wms_scale_denominator</ogc:Literal></ogc:Function>
                  <ogc:Literal>-1</ogc:Literal> <!-- do not display-->
                  <ogc:Literal>3000000</ogc:Literal>
                  <ogc:Literal>0</ogc:Literal>
                  <ogc:Literal>5000000</ogc:Literal>
                  <ogc:Literal>1</ogc:Literal>
                  <ogc:Literal>10000000</ogc:Literal>
                  <ogc:Literal>2</ogc:Literal>
                  <ogc:Literal>30000000</ogc:Literal>
                  <ogc:Literal>3</ogc:Literal>
                  <ogc:Literal>50000000</ogc:Literal>
                  <ogc:Literal>4</ogc:Literal>
                </ogc:Function>
              </ogc:PropertyIsEqualTo>
            </ogc:And>
          </ogc:Filter>
          <TextSymbolizer>
            <Geometry>
              <Function name="pointOnLine">
                <ogc:PropertyName>element</ogc:PropertyName>
                <ogc:Literal>0.5</ogc:Literal>
              </Function>
            </Geometry>
            <Label>
              <ogc:PropertyName>label</ogc:PropertyName>
            </Label>
            <Font>
              <CssParameter name="font-family">Noto Sans</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
              <CssParameter name="font-style">normal</CssParameter>
            </Font>
            <LabelPlacement>
              <PointPlacement>
                <AnchorPoint>
                  <AnchorPointX>0.5</AnchorPointX>
                  <AnchorPointY>0.5</AnchorPointY>
                </AnchorPoint>
              </PointPlacement>
            </LabelPlacement>
            <Halo>
              <Radius>1</Radius>
              <Fill>
                <CssParameter name="fill">#333333</CssParameter>
                <CssParameter name="fill-opacity">0.5</CssParameter>
              </Fill>
            </Halo>
            <Fill>
              <CssParameter name="fill">#dddddd</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>      
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>