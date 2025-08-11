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
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
    private final String baseApiUrl;
    private int pageSize = 5;

    @Autowired private final PinningServiceConfig config;

    @Autowired private PinningServiceLogger logger;

    private static final String SEARCH_PATH = "search/findByType?type=mapView";
    private static final String LAST_UPDATE_FILTER = "&lastUpdate=?";

    public ViewsClient(PinningServiceConfig config) {
        this.config = config;
        this.baseApiUrl = config.preferencesUrl();
        this.pageSize = config.preferencesPagesSize();
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
    public Iterable<ParsedView> fetchViews(Instant lastUpdatedFilter) {
        return () ->
                new Iterator<>() {
                    private String nextUrl = buildInitialUrl(lastUpdatedFilter);
                    private Iterator<ParsedView> currentBatch = Collections.emptyIterator();

                    @Override
                    public boolean hasNext() {
                        loadNextBatchIfNeeded();
                        return currentBatch.hasNext();
                    }

                    @Override
                    public ParsedView next() {
                        loadNextBatchIfNeeded();
                        return currentBatch.next();
                    }

                    private void loadNextBatchIfNeeded() {
                        if (currentBatch.hasNext()) {
                            return;
                        }
                        if (nextUrl == null) {
                            return;
                        }

                        try {
                            logger.log(Level.FINE, "Fetching views from: " + nextUrl);
                            String responseStr = restTemplate.getForObject(nextUrl, String.class);
                            ViewsResponse viewsResponse =
                                    objectMapper.readValue(responseStr, ViewsResponse.class);

                            // Convert current page's views to ParsedView objects
                            List<View> views = viewsResponse.getEmbedded().getPreferences();
                            List<ParsedView> parsedViews = parseAndSort(views, false);
                            logger.log(
                                    Level.INFO,
                                    String.format(
                                            "Retrieved %d views from the preferences endpoint",
                                            views.size()));

                            // Get "next" link for pagination
                            Map<String, ViewsResponse.Link> links = viewsResponse.getLinks();
                            ViewsResponse.Link nextLink =
                                    (links != null) ? links.get("next") : null;
                            nextUrl = (nextLink != null) ? nextLink.getHref() : null;

                            // Prepare iterator for this page
                            currentBatch = parsedViews.iterator();
                        } catch (Exception e) {
                            throw new RuntimeException("Error fetching paginated views", e);
                        }
                    }
                };
    }

    private String buildInitialUrl(Instant lastUpdatedFilter) {
        String url = baseApiUrl + SEARCH_PATH;
        if (lastUpdatedFilter != null) {
            logger.log(Level.FINE, "Filtering views by last update time: " + lastUpdatedFilter);
            String lastUpdatedFilterStr =
                    lastUpdatedFilter
                            .atOffset(ZoneOffset.UTC)
                            .format(DateTimeFormatter.ISO_DATE_TIME);
            url += LAST_UPDATE_FILTER.replace("?", lastUpdatedFilterStr);
        }
        if (pageSize > 0) {
            url += "&size=" + pageSize + "&page=0";
        }
        return url;
    }

    /**
     * Parses a list of View objects into ParsedView objects, filtering out non-event views.
     *
     * @param remoteViews The list of View objects to be parsed
     * @return A list of ParsedView objects containing the parsed and filtered view details
     * @throws IllegalArgumentException If JSON parsing fails or preference data is invalid
     */
    private List<ParsedView> parseAndSort(List<View> remoteViews, boolean sort)
            throws IllegalArgumentException {
        Stream<ParsedView> stream =
                remoteViews.stream().map(this::parseView).filter(this::isEventView);

        if (sort) {
            stream = stream.sorted(Comparator.comparing(ParsedView::getLastUpdate));
        }

        return stream.collect(Collectors.toList());
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

    public void setPageSize(int pageSize) {
        if (pageSize <= 0) {
            throw new IllegalArgumentException("Page size must be greater than zero");
        }
        this.pageSize = pageSize;
    }
}
