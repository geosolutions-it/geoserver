<?xml version="1.0" encoding="UTF-8"?>
<StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
    xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd">
  <UserLayer>
    <UserStyle>
      <Name>sst_composition</Name>
      <Title/>
      <FeatureTypeStyle>
        <Name>sst_composition</Name>
        <Rule>
          <RasterSymbolizer>
            <ChannelSelection>
              <GrayChannel>
                <SourceChannelName>1</SourceChannelName> 
              </GrayChannel>
            </ChannelSelection>
            <ColorMap>
              <ColorMapEntry color="#0000FF" quantity="270" opacity="0"/>
              <ColorMapEntry color="#0000FF" quantity="270.15" label_kelvin="270.15" label="-3 °C"/>
              <ColorMapEntry color="#71CEDA" quantity="285.15" label_kelvin="285.15" label="12 °C"/>
              <ColorMapEntry color="#FEFDB0" quantity="298.15" label_kelvin="298.15" label="25 °C"/>
              <ColorMapEntry color="#8B0604" quantity="318.15" label_kelvin="318.15" label="45 °C"/>
              <ColorMapEntry color="#8B0604" quantity="1000" opacity="0" />
            </ColorMap>
          </RasterSymbolizer>
        </Rule>
      </FeatureTypeStyle>
    </UserStyle>
  </UserLayer>
</StyledLayerDescriptor>