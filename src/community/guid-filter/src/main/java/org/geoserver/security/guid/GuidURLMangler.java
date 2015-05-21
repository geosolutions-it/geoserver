package org.geoserver.security.guid;

import java.util.Map;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.URLMangler;

/**
 * Adds the guid parameter in all backlinks
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GuidURLMangler implements URLMangler
{

    @Override
    public void mangleURL(StringBuilder baseURL, StringBuilder path, Map<String, String> kvp,
        URLType type)
    {
        Request request = Dispatcher.REQUEST.get();
        if (request != null) {
            String guid = (String) request.getRawKvp().get("guid");
            if (guid != null) {
                kvp.put("guid", guid);
            }
        }
    }

}