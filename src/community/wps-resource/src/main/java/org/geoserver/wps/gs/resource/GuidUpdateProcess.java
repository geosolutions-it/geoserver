/**
 * 
 */
package org.geoserver.wps.gs.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.security.guid.GuidRule;
import org.geoserver.security.guid.GuidRuleDao;
import org.geoserver.wps.gs.resource.model.GuidTemplateModel;
import org.geoserver.wps.process.RawData;
import org.geoserver.wps.process.StringRawData;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.process.ProcessException;
import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.gs.GSProcess;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.util.ProgressListener;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author alessio.fabiani
 * 
 */
@SuppressWarnings("deprecation")
@DescribeProcess(title = "Guid Update Process", description = "Allow to store/update GUIDs on GeoServer.")
public class GuidUpdateProcess implements GSProcess {

    /** The LOGGER. */
    private static final Logger LOGGER = Logging.getLogger(GuidUpdateProcess.class);

    /** The catalog. */
    private final Catalog catalog;

    private final GuidRuleDao rulesDao;

    public GuidUpdateProcess(GeoServer geoServer, GuidRuleDao rules) {
        Utilities.ensureNonNull("geoServer", geoServer);
        this.catalog = geoServer.getCatalog();
        this.rulesDao = rules;
    }

    @DescribeResult(name = "result", description = "XML describing the Resources to load", type = String.class)
    public String execute(
            @DescribeParameter(name = "guid", min = 1, description = "The GUID to be used") String guid,
            @DescribeParameter(name = "layer", min = 0, description = "List of Layers and Filters associated with the GUID", meta = {
                    "mimeTypes=application/json,text/xml" }, collectionType = RawData.class) final List<RawData> layers,
            final ProgressListener progressListener) throws ProcessException {

        try {
            // Extracting Layers from JSON
            List<GuidTemplateModel> encodedLayers = new ArrayList<GuidTemplateModel>();

            if (layers != null && layers.size() > 0) {
                for (RawData layer : layers) {
                    if (layer != null && layer instanceof StringRawData
                            && ((StringRawData) layer).getData() != null) {
                        JsonFactory jsonF = new JsonFactory();
                        JsonParser jsonP = jsonF.createParser(((StringRawData) layer).getData());
                        JsonToken token = jsonP.nextToken(); // will return JsonToken.START_ARRAY

                        while (token != JsonToken.END_ARRAY) {
                            token = jsonP.getCurrentToken();
                            if (token == null || token == JsonToken.END_ARRAY)
                                break;

                            token = jsonP.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                            String fieldname = jsonP.getCurrentName();

                            // Parsing Metocs
                            if (fieldname != null && fieldname.equalsIgnoreCase("Layer")) {
                                jsonP.nextToken(); // move to value, or START_OBJECT/START_ARRAY
                                token = jsonP.getCurrentToken();
                                ObjectMapper mapper = new ObjectMapper();
                                GuidTemplateModel layerObj = mapper.readValue(jsonP,
                                        GuidTemplateModel.class);

                                encodedLayers.add(layerObj);
                            }
                        }

                        jsonP.close();
                    }
                }

                List<GuidRule> rules = rulesDao.getRules(guid);
                // if the guid is specified but not knows, by spec we throw an exception
                if (rules != null && !rules.isEmpty()) {
                    rulesDao.clearRules(guid);
                }

                // update rules for the selected layers
                if (layers != null && layers.size() > 0) {

                    rulesDao.clearRules(guid);

                    for (GuidTemplateModel rr : encodedLayers) {
                        GuidRule rule = new GuidRule(guid, rr.getLayerName(), rr.getUserId(),
                                (rr.getFilter() != null ? ECQL.toFilter(rr.getFilter())
                                        : Filter.INCLUDE));

                        rulesDao.addRule(rule);
                    }
                }
            } else {
                rulesDao.clearRules(guid);
            }
        } catch (Exception e) {
            throw new ProcessException(e);
        }

        return guid;
    }
}