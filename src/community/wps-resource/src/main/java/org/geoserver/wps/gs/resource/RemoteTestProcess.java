/**
 * 
 */
package org.geoserver.wps.gs.resource;

import java.util.logging.Logger;

import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.logging.Logging;
import org.opengis.util.ProgressListener;

/**
 * @author alessio.fabiani
 *
 */
@SuppressWarnings("deprecation")
@DescribeProcess(title = "Resource Loader Process", description = "Downloads Layer Stream and provides a ZIP.")
public class RemoteTestProcess implements GSProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(ResourceLoaderProcess.class);

    @DescribeResult(name = "result", description = "XML describing the Resources to load")
    public String execute(final ProgressListener progressListener) throws ProcessException {
    	return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
    			+ "<resources>"
    			+ " <resource class=\"vectorialLayer\">"
    			+ "  <name>way_points_6beb3206af66f6c8274cf888e85b2b81_e845371bd131ec4d737b6d619187d208</name>"
    			+ "   <persistent>true</persistent>"
    			+ "		<defaultStyle>"
    			+ "			<name>point</name>"
    			+ "			<filename>point.sld</filename>"
    			+ "		</defaultStyle>"
    			+ "		<title>A Test Vectorial Resource</title>"
    			+ "		<abstract>This is a Test Vectorial Resource.</abstract>"
    			+ "		<keywords>"
    			+ "			<string>census</string>"
    			+ "			<string>united</string>"
    			+ "			<string>boundaries</string>"
    			+ "			<string>state</string>"
    			+ "			<string>states</string>"
    			+ "		</keywords>"
    			+ "		<nativeCRS>GEOGCS[\"GCS_WGS_1984\","
    			+ "			DATUM[\"WGS_1984\","
    			+ "			SPHEROID[\"WGS_1984\", 6378137.0, 298.257223563]],"
    			+ "			PRIMEM[\"Greenwich\","
    			+ "			0.0],"
    			+ "			UNIT[\"degree\", 0.017453292519943295],"
    			+ "			AXIS[\"Longitude\", EAST],"
    			+ "			AXIS[\"Latitude\", NORTH]]"
    			+ "		</nativeCRS>"
    			+ "		<srs>EPSG:4326</srs>"
    			+ "		<nativeBoundingBox>"
    			+ "			<minx>-124.73142200000001</minx>"
    			+ "			<maxx>-66.969849</maxx>"
    			+ "			<miny>24.955967</miny>"
    			+ "			<maxy>49.371735</maxy>"
    			+ "			<crs>EPSG:4326</crs>"
    			+ "		</nativeBoundingBox>"
    			+ "		<metadata>"
    			+ "			<cacheAgeMax>3600</cacheAgeMax>"
    			+ "			<kml.regionateFeatureLimit>10</kml.regionateFeatureLimit>"
    			+ "			<indexingEnabled>false</indexingEnabled>"
    			+ "			<cachingEnabled>true</cachingEnabled>"
    			+ "			<dirName>states</dirName>"
    			+ "		</metadata>"
    			+ "		<translateContext>"
    			+ "			<item class=\"dataStore\" order=\"0\">"
    			+ "				<store>"
    			+ "					<url>file:/D:/tmp/OAA_temp/6beb3206af66f6c8274cf888e85b2b81_e845371bd131ec4d737b6d619187d208/waypoints.csv</url>"
    			+ "				</store>"
    			+ "			</item>"
    			+ "			<item class=\"transform\" order=\"1\""
    			+ "				type=\"org.geoserver.importer.transform.AttributesToPointGeometryTransform\">"
    			+ "				<transform>"
    			+ "					<latField>Lat</latField>"
    			+ "					<lngField>Lon</lngField>"
    			+ "					<pointFieldName>location</pointFieldName>"
    			+ "					<geometryFactory>"
    			+ "						<coordinateSequenceFactory"
    			+ "							class=\"com.vividsolutions.jts.geom.impl.CoordinateArraySequenceFactory\" />"
    			+ "						<SRID>4326</SRID>"
    			+ "					</geometryFactory>"
    			+ "				</transform>"
    			+ "			</item>"
    			+ "			<item class=\"dataStore\" order=\"2\">"
    			+ "				<store>"
    			+ "					<url>file:way_points_6beb3206af66f6c8274cf888e85b2b81_e845371bd131ec4d737b6d619187d208.shp</url>"
    			+ "				</store>"
    			+ "			</item>"
    			+ "		</translateContext>"
    			+ "	</resource>"
    			+ "</resources>";
    }

}
