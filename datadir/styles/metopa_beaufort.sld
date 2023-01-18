<?xml version="1.0" encoding="ISO-8859-1"?>
<StyledLayerDescriptor version="1.0.0"
    xsi:schemaLocation="http://www.opengis.net/sld http://schemas.opengis.net/sld/1.0.0/StyledLayerDescriptor.xsd"
    xmlns="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc"
    xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
    <NamedLayer>
      <Name>MetopA ASCAT Shape</Name>
      <UserStyle>
        <Title>Beaufort Scale</Title>
        <FeatureTypeStyle>
          <Rule>
            <Title>LOD 0</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>0</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
			<MinScaleDenominator>200000000</MinScaleDenominator>
			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
                                <ogc:Function name="Categorize">
                                 <!-- Value to transform -->
                                 <ogc:Function name="if_then_else">
                                        <ogc:Function name="isNull">
                                           <ogc:PropertyName>wind_speed</ogc:PropertyName>
                                        </ogc:Function>
                                     <ogc:Literal>10</ogc:Literal>
                                     <ogc:PropertyName>wind_speed</ogc:PropertyName>
                                  </ogc:Function>

                                 <!-- Output values and thresholds -->
                                 <!-- Output values are in m/s -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                               </ogc:Function>                 
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>8</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>                
			  </PointSymbolizer> 
          </Rule>
          <Rule>
            <Title>LOD 0-2</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                         <ogc:Literal>0</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
			<MinScaleDenominator>100000000</MinScaleDenominator>
            <MaxScaleDenominator>200000000</MaxScaleDenominator> 
			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
                                <ogc:Function name="Categorize">
                                 <!-- Value to transform -->
                                 <ogc:PropertyName>wind_speed</ogc:PropertyName>

                                 <!-- Output values and thresholds -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                               </ogc:Function>                 
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>8</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>                
			  </PointSymbolizer> 
          </Rule>
          <Rule>
            <Title>LOD 0-4</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>0</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
			<MinScaleDenominator>60000000</MinScaleDenominator> 
       		<MaxScaleDenominator>100000000</MaxScaleDenominator> 
			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
                                <ogc:Function name="Categorize">
                                 <!-- Value to transform -->
                                 <ogc:PropertyName>wind_speed</ogc:PropertyName>

                                 <!-- Output values and thresholds -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                               </ogc:Function>                 
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>8</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>                
			  </PointSymbolizer> 
          </Rule>
		  <Rule>
            <Title>LOD 0-4</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>0</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
			<MinScaleDenominator>25000000</MinScaleDenominator> 
       		<MaxScaleDenominator>60000000</MaxScaleDenominator> 
			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
                                <ogc:Function name="Categorize">
                                 <!-- Value to transform -->
                                 <ogc:PropertyName>wind_speed</ogc:PropertyName>

                                 <!-- Output values and thresholds -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                               </ogc:Function>                 
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>10</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>                
			  </PointSymbolizer> 
          </Rule>
          <Rule>
            <Title>LOD 0-5</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>5</ogc:Literal>
                      	 <ogc:Literal>0</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
			<MinScaleDenominator>15000000</MinScaleDenominator>			
			<MaxScaleDenominator>25000000</MaxScaleDenominator> 
			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
                           <ogc:Function name="Categorize">
                             <!-- Value to transform -->
                             <ogc:PropertyName>wind_speed</ogc:PropertyName>

                             <!-- Output values and thresholds -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                           </ogc:Function>                    
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>12</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>
			  </PointSymbolizer>
          </Rule>
		  <Rule>
            <Title>LOD 0-6</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>5</ogc:Literal>
                      	 <ogc:Literal>6</ogc:Literal>
                      	 <ogc:Literal>0</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
            
			<MinScaleDenominator>8000000</MinScaleDenominator>
       		<MaxScaleDenominator>15000000</MaxScaleDenominator> 

			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
								<ogc:Function name="Categorize">
                             <!-- Value to transform -->
                             <ogc:PropertyName>wind_speed</ogc:PropertyName>

                             <!-- Output values and thresholds -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                           </ogc:Function>
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>12</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>
			  </PointSymbolizer>
          </Rule>	
		  <Rule>
            <Title>LOD 0-8</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>5</ogc:Literal>
                      	 <ogc:Literal>6</ogc:Literal>
                         <ogc:Literal>7</ogc:Literal>
                      	 <ogc:Literal>0</ogc:Literal>
						 <ogc:Literal>8</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>

			<MinScaleDenominator>3000000</MinScaleDenominator> 
       		<MaxScaleDenominator>8000000</MaxScaleDenominator> 

			  <PointSymbolizer>
				  <Graphic>
					  <Mark>
						  <WellKnownName>extshape://sarrow</WellKnownName>
						  <Fill>
							  <CssParameter name="fill">
								<ogc:Function name="Categorize">
                             <!-- Value to transform -->
                             <ogc:PropertyName>wind_speed</ogc:PropertyName>

                             <!-- Output values and thresholds -->
                                 <ogc:Literal>#FFFFFF</ogc:Literal>

                                 <ogc:Literal>0.5</ogc:Literal>
                                 <ogc:Literal>#AEF1F9</ogc:Literal>
                                 <ogc:Literal>1.5</ogc:Literal>
                                 <ogc:Literal>#96F7DC</ogc:Literal>
                                 <ogc:Literal>3.3</ogc:Literal>
                                 <ogc:Literal>#96F7B4</ogc:Literal>
                                 <ogc:Literal>5.5</ogc:Literal>
                                 <ogc:Literal>#6FF46F</ogc:Literal>
                                 <ogc:Literal>7.9</ogc:Literal>
                                 <ogc:Literal>#73ED12</ogc:Literal>
                                 <ogc:Literal>10.7</ogc:Literal>
                                 <ogc:Literal>#A4ED12</ogc:Literal>
                                 <ogc:Literal>13.8</ogc:Literal>
                                 <ogc:Literal>#DAED12</ogc:Literal>
                                 <ogc:Literal>17.1</ogc:Literal>
                                 <ogc:Literal>#EDC212</ogc:Literal>
                                 <ogc:Literal>20.7</ogc:Literal>
                                 <ogc:Literal>#ED8F12</ogc:Literal>
                                 <ogc:Literal>24.4</ogc:Literal>
                                 <ogc:Literal>#ED6312</ogc:Literal>
                                 <ogc:Literal>28.4</ogc:Literal>
                                 <ogc:Literal>#ED2912</ogc:Literal>
                                 <ogc:Literal>32.6</ogc:Literal>
                                 <ogc:Literal>#D5102D</ogc:Literal>
                           </ogc:Function>
							  </CssParameter>
						  </Fill>
					  </Mark>
					  <Size>
						<ogc:Literal>12</ogc:Literal>
					  </Size>
					  <Rotation>
						  <ogc:PropertyName>wind_direc</ogc:PropertyName>
					  </Rotation>
				  </Graphic>
			  </PointSymbolizer>
          </Rule>	
          <Rule>
            <Title>LOD 0-255</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>5</ogc:Literal>
                      	 <ogc:Literal>6</ogc:Literal>
                         <ogc:Literal>7</ogc:Literal>
                      	 <ogc:Literal>0</ogc:Literal>
						 <ogc:Literal>8</ogc:Literal>
                      	 <ogc:Literal>255</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
            
			<MinScaleDenominator>1000000</MinScaleDenominator>
       		<MaxScaleDenominator>3000000</MaxScaleDenominator> 

<PointSymbolizer>
                  <Graphic>
                     <Mark>
                       <WellKnownName>windbarbs://default(
<ogc:Function name="if_then_else">
      <ogc:Function name="isNull">
         <ogc:PropertyName>wind_speed</ogc:PropertyName>
      </ogc:Function>
   <ogc:Literal>10</ogc:Literal>
   <ogc:PropertyName>wind_speed</ogc:PropertyName>
</ogc:Function>
                        )[m/s]?hemisphere=
                         <ogc:Function name="if_then_else">
                          <ogc:Function name="greaterThan">
                            <ogc:Function name="getY">
                              <ogc:PropertyName>the_geom</ogc:PropertyName>
                            </ogc:Function>
                            <ogc:Literal>0</ogc:Literal>
                          </ogc:Function>
                          <ogc:Literal>n</ogc:Literal>
                          <ogc:Literal>s</ogc:Literal>
                        </ogc:Function>
                       </WellKnownName>
                       <Stroke>
                         <CssParameter name="stroke">
                           <ogc:Function name="Categorize">
                             <!-- Value to transform -->
<ogc:Function name="if_then_else">
      <ogc:Function name="isNull">
         <ogc:PropertyName>wind_speed</ogc:PropertyName>
      </ogc:Function>
   <ogc:Literal>10</ogc:Literal>
   <ogc:PropertyName>wind_speed</ogc:PropertyName>
</ogc:Function>

                             <!-- Output values and thresholds -->
                             <ogc:Literal>#FFFFFF</ogc:Literal>

                             <ogc:Literal>0.5</ogc:Literal>
                             <ogc:Literal>#AEF1F9</ogc:Literal>
                             <ogc:Literal>1.5</ogc:Literal>
                             <ogc:Literal>#96F7DC</ogc:Literal>
                             <ogc:Literal>3.3</ogc:Literal>
                             <ogc:Literal>#96F7B4</ogc:Literal>
                             <ogc:Literal>5.5</ogc:Literal>
                             <ogc:Literal>#6FF46F</ogc:Literal>
                             <ogc:Literal>7.9</ogc:Literal>
                             <ogc:Literal>#73ED12</ogc:Literal>
                             <ogc:Literal>10.7</ogc:Literal>
                             <ogc:Literal>#A4ED12</ogc:Literal>
                             <ogc:Literal>13.8</ogc:Literal>
                             <ogc:Literal>#DAED12</ogc:Literal>
                             <ogc:Literal>17.1</ogc:Literal>
                             <ogc:Literal>#EDC212</ogc:Literal>
                             <ogc:Literal>20.7</ogc:Literal>
                             <ogc:Literal>#ED8F12</ogc:Literal>
                             <ogc:Literal>24.4</ogc:Literal>
                             <ogc:Literal>#ED6312</ogc:Literal>
                             <ogc:Literal>28.4</ogc:Literal>
                             <ogc:Literal>#ED2912</ogc:Literal>
                             <ogc:Literal>32.6</ogc:Literal>
                             <ogc:Literal>#D5102D</ogc:Literal>
                           </ogc:Function>
                         </CssParameter>
                         <CssParameter name="stroke-width">1</CssParameter>
                       </Stroke>
                    </Mark>
                    <Size>
                      <ogc:Function name="Categorize">
                        <!-- Value to transform -->
                        <ogc:PropertyName>wind_speed</ogc:PropertyName>
                        <ogc:Literal>8</ogc:Literal>
                        <ogc:Literal>1.543333332</ogc:Literal>
                        <ogc:Literal>20</ogc:Literal>
                      </ogc:Function>
                    </Size>
                    <Rotation>
                      <ogc:PropertyName>wind_direc</ogc:PropertyName>
                    </Rotation>
                  </Graphic>
               </PointSymbolizer>
            	
          </Rule>	
          <Rule>
            <Title>LOD 0-255 Bigger</Title>
			<ogc:Filter>
               <ogc:PropertyIsEqualTo>
                    <ogc:Function name="in">
                         <ogc:PropertyName>lod</ogc:PropertyName>
                         <ogc:Literal>1</ogc:Literal>
                         <ogc:Literal>2</ogc:Literal>
                      	 <ogc:Literal>3</ogc:Literal>
                         <ogc:Literal>4</ogc:Literal>
                         <ogc:Literal>5</ogc:Literal>
                      	 <ogc:Literal>6</ogc:Literal>
                         <ogc:Literal>7</ogc:Literal>
                      	 <ogc:Literal>0</ogc:Literal>
						 <ogc:Literal>8</ogc:Literal>
                      	 <ogc:Literal>255</ogc:Literal>
                    </ogc:Function>
                    <ogc:Literal>true</ogc:Literal>
               </ogc:PropertyIsEqualTo>
            </ogc:Filter>
            <!--
			<MinScaleDenominator>3000000</MinScaleDenominator> -->
       		<MaxScaleDenominator>1000000</MaxScaleDenominator> 
            <PointSymbolizer>
                  <Graphic>
                     <Mark>
                       <WellKnownName>windbarbs://default(
<ogc:Function name="if_then_else">
      <ogc:Function name="isNull">
         <ogc:PropertyName>wind_speed</ogc:PropertyName>
      </ogc:Function>
   <ogc:Literal>10</ogc:Literal>
   <ogc:PropertyName>wind_speed</ogc:PropertyName>
</ogc:Function>
                         )[m/s]?hemisphere=
                         <ogc:Function name="if_then_else">
                          <ogc:Function name="greaterThan">
                            <ogc:Function name="getY">
                              <ogc:PropertyName>the_geom</ogc:PropertyName>
                            </ogc:Function>
                            <ogc:Literal>0</ogc:Literal>
                          </ogc:Function>
                          <ogc:Literal>n</ogc:Literal>
                          <ogc:Literal>s</ogc:Literal>
                        </ogc:Function>
                       </WellKnownName>
                       <Stroke>
                         <CssParameter name="stroke">
                           <ogc:Function name="Categorize">
                             <!-- Value to transform -->
<ogc:Function name="if_then_else">
      <ogc:Function name="isNull">
         <ogc:PropertyName>wind_speed</ogc:PropertyName>
      </ogc:Function>
   <ogc:Literal>10</ogc:Literal>
   <ogc:PropertyName>wind_speed</ogc:PropertyName>
</ogc:Function>
                             <!-- Output values and thresholds -->
							<ogc:Literal>#FFFFFF</ogc:Literal>

                             <ogc:Literal>0.5</ogc:Literal>
                             <ogc:Literal>#AEF1F9</ogc:Literal>
                             <ogc:Literal>1.5</ogc:Literal>
                             <ogc:Literal>#96F7DC</ogc:Literal>
                             <ogc:Literal>3.3</ogc:Literal>
                             <ogc:Literal>#96F7B4</ogc:Literal>
                             <ogc:Literal>5.5</ogc:Literal>
                             <ogc:Literal>#6FF46F</ogc:Literal>
                             <ogc:Literal>7.9</ogc:Literal>
                             <ogc:Literal>#73ED12</ogc:Literal>
                             <ogc:Literal>10.7</ogc:Literal>
                             <ogc:Literal>#A4ED12</ogc:Literal>
                             <ogc:Literal>13.8</ogc:Literal>
                             <ogc:Literal>#DAED12</ogc:Literal>
                             <ogc:Literal>17.1</ogc:Literal>
                             <ogc:Literal>#EDC212</ogc:Literal>
                             <ogc:Literal>20.7</ogc:Literal>
                             <ogc:Literal>#ED8F12</ogc:Literal>
                             <ogc:Literal>24.4</ogc:Literal>
                             <ogc:Literal>#ED6312</ogc:Literal>
                             <ogc:Literal>28.4</ogc:Literal>
                             <ogc:Literal>#ED2912</ogc:Literal>
                             <ogc:Literal>32.6</ogc:Literal>
                             <ogc:Literal>#D5102D</ogc:Literal>
                           </ogc:Function>
                         </CssParameter>
                         <CssParameter name="stroke-width">1</CssParameter>
                       </Stroke>
                    </Mark>
                    <Size>
                      <ogc:Function name="Categorize">
                        <!-- Value to transform -->
                        <ogc:PropertyName>wind_speed</ogc:PropertyName>
                        <ogc:Literal>8</ogc:Literal>
                        <ogc:Literal>1.543333332</ogc:Literal>
                        <ogc:Literal>32</ogc:Literal>
                      </ogc:Function>
                    </Size>
                    <Rotation>
                      <ogc:PropertyName>wind_direc</ogc:PropertyName>
                    </Rotation>
                  </Graphic>
               </PointSymbolizer>
            	
          </Rule>
        </FeatureTypeStyle>
      </UserStyle>
    </NamedLayer>
</StyledLayerDescriptor>