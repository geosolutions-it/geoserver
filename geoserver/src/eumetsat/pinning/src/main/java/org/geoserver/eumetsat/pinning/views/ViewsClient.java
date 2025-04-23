/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning.views;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.eumetsat.pinning.PinningServiceLogger;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
/**
 * Client for fetching and parsing map views from a preferences endpoint.
 *
 * <p>This class handles retrieving view data via REST API and converting view preferences into a
 * format usable by the GeoServer EUMETSAT pinning service.
 */
public class ViewsClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final Catalog catalog;
    private final String baseApiUrl;

    @Autowired private final PinningServiceConfig config;

    @Autowired private PinningServiceLogger logger;

    private static final String SEARCH_PATH = "search/findByType?type=mapView";
    private static final String LAST_UPDATE_FILTER = "&lastUpdate=?";

    public ViewsClient(Catalog catalog, PinningServiceConfig config) {
        this.catalog = catalog;
        this.config = config;
        this.baseApiUrl = config.preferencesUrl();
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Fetches map views from the preferences endpoint, optionally filtered by last update time.
     *
     * @param lastUpdatedFilter Optional timestamp to filter views by their last update time
     * @return A list of View objects retrieved from the preferences endpoint
     * @throws Exception If there are issues with REST request or JSON parsing
     */
    public List<ParsedView> fetchViews(Instant lastUpdatedFilter) throws Exception {
        String request = baseApiUrl + SEARCH_PATH;
        if (lastUpdatedFilter != null) {
            logger.log(Level.FINE, "Filtering views by last update time: " + lastUpdatedFilter);
            String lastUpdatedFilterStr =
                    lastUpdatedFilter
                            .atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_DATE_TIME);
            request += LAST_UPDATE_FILTER.replace("?", lastUpdatedFilterStr);
        }
        logger.log(Level.FINE, "Fetching views from the preferences endpoint: " + request);

        String response = restTemplate.getForObject(request, String.class);
        ViewsResponse viewsResponse = objectMapper.readValue(response, ViewsResponse.class);
        List<View> views = viewsResponse.getEmbedded().getPreferences();
        logger.log(
                Level.INFO,
                String.format("Retrieved %d views from the preferences endpoint", views.size()));
        return parseAndSort(views);
    }

    private List<ParsedView> parseAndSort(List<View> remoteViews) throws IllegalArgumentException {
        return remoteViews.stream()
                .map(this::parseView)
                .filter(this::isEventView)
                .sorted(Comparator.comparing(ParsedView::getLastUpdate)) // Sort by lastUpdate
                .collect(Collectors.toList());
    }
    /**
     * Parses a View object into a ParsedView by extracting and transforming view preferences.
     *
     * @param view The View object to be parsed
     * @return A ParsedView containing extracted view details
     * @throws IllegalArgumentException If JSON parsing fails or preference data is invalid
     */
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
            String drivingLayer = preference.getDrivingLayer();
            String lastUpdate = view.getLastUpdate();
            if (!lastUpdate.endsWith("Z")) {
                lastUpdate += "Z";
            }

            return new ParsedView(
                    view.getViewId(),
                    drivingLayer,
                    layers,
                    Instant.parse(time),
                    timeMode,
                    Instant.parse(lastUpdate),
                    view.getDisabled());
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Exception occurred while parsing the JSON", e);
        }
    }

    private boolean isEventView(ParsedView parsed) {
        String timeMode = parsed.getTimeMode();
        boolean isEventView = "absolute".equalsIgnoreCase(timeMode);
        if (!isEventView) {
            // Only event views are pinned.
            // Live views (with mode=latest) don't need that
            if ("latest".equalsIgnoreCase(timeMode)) {
                logger.log(
                        Level.FINE,
                        String.format(
                                "View with id=%s will be skipped since it's a live view",
                                parsed.getViewId()));
            } else {
                logger.log(
                        Level.WARNING,
                        String.format(
                                "View with id=%s will be skipped since %s mode wasn't recognized",
                                parsed.getViewId(), timeMode));
            }
        }
        return isEventView;
    }
}
