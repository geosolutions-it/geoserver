<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
                       xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
                       xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
                       xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">

  <NamedLayer>
    <Name>RDT</Name>
    <UserStyle>
      <Title>Polygons and vectors</Title>
      <FeatureTypeStyle>
        <Rule>
          <PolygonSymbolizer>
            <Stroke>
              <CssParameter name="stroke">#FF0000</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
        </Rule>
        <Rule>
          <MaxScaleDenominator>30000000</MaxScaleDenominator>
          <ogc:Filter>
            <ogc:PropertyIsGreaterThan>
              <ogc:Mul>
                <ogc:Div>
                  <ogc:Function name="area">
                    <ogc:PropertyName>the_geom</ogc:PropertyName>
                  </ogc:Function>
                  <ogc:Function name="env">
                    <ogc:Literal>wms_scale_denominator</ogc:Literal>
                  </ogc:Function>
                </ogc:Div>
                <ogc:Literal>80000000</ogc:Literal>
              </ogc:Mul>
              <ogc:Literal>1</ogc:Literal>
            </ogc:PropertyIsGreaterThan>
          </ogc:Filter>
          <TextSymbolizer>
            <Geometry>
              <ogc:Function name="centroid">
                <ogc:PropertyName>the_geom</ogc:PropertyName>
              </ogc:Function>
            </Geometry>  
            <Label>
              <ogc:PropertyName>BTmin</ogc:PropertyName>
              <!--ogc:Mul>
                <ogc:Div>
                  <ogc:Function name="area">
                    <ogc:PropertyName>the_geom</ogc:PropertyName>
                  </ogc:Function>
                  <ogc:Function name="env">
                    <ogc:Literal>wms_scale_denominator</ogc:Literal>
                  </ogc:Function>
                </ogc:Div>
                <ogc:Literal>10000000</ogc:Literal>
              </ogc:Mul-->
            </Label>
            <Font>
              <CssParameter name="font-family">DejaVu Sans</CssParameter>
              <CssParameter name="font-size">12</CssParameter>
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
              <Radius>2</Radius>
              <Fill>
                <CssParameter name="fill">#FFFFFF</CssParameter>
              </Fill>
            </Halo>
            <Fill>
              <CssParameter name="fill">#FF0000</CssParameter>
            </Fill>
          </TextSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>