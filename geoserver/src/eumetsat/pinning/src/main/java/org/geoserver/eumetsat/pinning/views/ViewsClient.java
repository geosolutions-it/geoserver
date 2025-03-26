package org.geoserver.eumetsat.pinning.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class ViewsClient {

    private static final Logger LOGGER = Logging.getLogger(ViewsClient.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Catalog catalog;
    private final String baseApiUrl;

    @Autowired private final PinningServiceConfig config;

    private static final String SEARCH_PATH = "search/findByType?type=mapView";
    private static final String LAST_UPDATE_FILTER = "&lastUpdate=?";

    public ViewsClient(Catalog catalog, PinningServiceConfig config) {
        this.catalog = catalog;
        this.config = config;
        this.baseApiUrl = config.preferencesUrl();
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    public List<View> fetchViews(String lastUpdatedFilter) throws Exception {
        String request = baseApiUrl + SEARCH_PATH;
        if (lastUpdatedFilter != null) {
            request += LAST_UPDATE_FILTER.replace("?", lastUpdatedFilter);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Fetching views from the preferences endpoint: " + request);
        }

        String response = restTemplate.getForObject(request, String.class);
        ViewsResponse viewsResponse = objectMapper.readValue(response, ViewsResponse.class);
        return viewsResponse.getEmbedded().getPreferences();
    }

    public ParsedView parseView(View view) throws IllegalArgumentException {
        // Convert preference JSON string to Preference object
        Preference preference = null;
        try {
            preference = objectMapper.readValue(view.getPreferenceJson(), Preference.class);

            // Extract time and layers
            List<String> layers =
                    preference.getLayers().stream().map(Layer::getId).collect(Collectors.toList());
            TimeInfo timeInfo = preference.getTime();
            String time = timeInfo.getValue();
            String timeMode = timeInfo.getMode();
            String lastUpdate = view.getLastUpdate();
            if (!lastUpdate.endsWith("Z")) {
                lastUpdate += "Z";
            }

            return new ParsedView(
                    view.getViewId(),
                    layers,
                    Instant.parse(time),
                    timeMode,
                    Instant.parse(lastUpdate),
                    view.getDisabled());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Exception occurred while parsing the JSON", e);
        }
    }
}
