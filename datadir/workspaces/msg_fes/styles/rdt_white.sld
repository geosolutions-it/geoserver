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
              <CssParameter name="stroke">#FFFFFF</CssParameter>
              <CssParameter name="stroke-width">2</CssParameter>
            </Stroke>
          </PolygonSymbolizer>
          <!-- PointSymbolizer>
            <Graphic>

              <Mark>
                <WellKnownName>line</WellKnownName>
                <Stroke>
                  <CssParameter name="stroke">#ff0000</CssParameter>
                  <CssParameter name="stroke-width">2</CssParameter>
                </Stroke>
              </Mark>
              <Size>
                <ogc:PropertyName>MvtSpeed</ogc:PropertyName>
              </Size>
              <Rotation>
                <ogc:Sub>
                  <ogc:Literal>90</ogc:Literal>
                  <ogc:PropertyName>MvtDirecti</ogc:PropertyName>
                </ogc:Sub>
              </Rotation>
              <AnchorPoint>
                <AnchorPointX>0</AnchorPointX>
                <AnchorPointY>0</AnchorPointY>
              </AnchorPoint>
            </Graphic>
          </PointSymbolizer -->
        </Rule>

      </FeatureTypeStyle>
    </UserStyle>
  </NamedLayer>
</StyledLayerDescriptor>