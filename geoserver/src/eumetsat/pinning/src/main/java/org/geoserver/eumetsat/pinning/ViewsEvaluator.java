package org.geoserver.eumetsat.pinning;

import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Level;
import javax.annotation.PostConstruct;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.geoserver.eumetsat.pinning.views.ParsedView;
import org.geoserver.eumetsat.pinning.views.ViewRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewsEvaluator {

    public class LayersUpdate {

        private Set<String> layersBefore;
        private Set<String> layersAfter;
        private Set<String> removed;
        private Set<String> added;
        private Set<String> unchanged;

        public LayersUpdate(List<String> layersBefore, List<String> layersAfter) {
            this.layersBefore = new HashSet<>(layersBefore);
            this.layersAfter = new HashSet<>(layersAfter);

            // removed layers (present in layersBefore but not in layersAfter)
            removed = new HashSet<>(layersBefore);
            removed.removeAll(layersAfter);
            if (!removed.isEmpty()) {
                logger.log(
                        Level.FINER,
                        "The following layers have been removed in the updated view: "
                                + String.join(",", removed));
            }

            // added layers (present in layersAfter but not in layersBefore)
            added = new HashSet<>(layersAfter);
            added.removeAll(layersBefore);
            if (!added.isEmpty()) {
                logger.log(
                        Level.FINER,
                        "The following layers have been added in the updated view: "
                                + String.join(",", added));
            }

            // unchanged layers (present in both layersBefore and layersAfter)
            unchanged = new HashSet<>(layersBefore);
            unchanged.retainAll(layersAfter);
        }

        public Set<String> getRemoved() {
            return removed;
        }

        public Set<String> getAdded() {
            return added;
        }

        public Set<String> getUnchanged() {
            return unchanged;
        }
    }

    public static final String RUNS_TABLE = "pinning.pinning_run";

    public static final String VIEWS_TABLE = "pinning.views";

    private static final String TRUNCATE_TABLE_VIEWS = "TRUNCATE table " + VIEWS_TABLE;

    private static final String GET_LAST_RUN_QUERY = "SELECT last_run from " + RUNS_TABLE;

    private static final String GET_VIEW_QUERY =
            "SELECT * FROM " + VIEWS_TABLE + " WHERE view_id = ?";

    private static final String UPDATE_LAST_RUN_QUERY =
            "INSERT INTO "
                    + RUNS_TABLE
                    + "(id, last_run)\n"
                    + "VALUES (1, ?)\n"
                    + "ON CONFLICT (id)\n"
                    + "DO UPDATE SET last_run = EXCLUDED.last_run;";

    private Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    @Autowired private LayersMapper layersMapper;

    @Autowired private PinningServiceConfig config;

    @Autowired private PinningServiceLogger logger;

    private Connection connection;

    private Duration duration;

    private PinBatcher pinBatcher;

    private ViewsBatcher viewsBatcher;

    @PostConstruct
    private void initDuration() {
        // Splitting the provided pinning minutes in half (which means multiplying minutes by 30
        // seconds)
        duration = Duration.ofSeconds(config.pinningMinutes() * 30);
    }

    public void init(Connection connection) throws SQLException {
        this.connection = connection;
        this.pinBatcher = new PinBatcher(connection, config.batchSize());
        this.viewsBatcher = new ViewsBatcher(connection, config.batchSize());
    }

    public void release() throws RuntimeException {
        SQLException firstException = null;
        try {
            this.pinBatcher.release();
        } catch (SQLException e) {
            firstException = e;
        }

        try {
            this.viewsBatcher.release();
        } catch (SQLException e) {
            if (firstException != null) {
                firstException.addSuppressed(e);
            } else {
                firstException = e;
            }
        }

        if (firstException != null) {
            throw new RuntimeException(firstException);
        }
    }

    public void flushBatches() throws SQLException {
        viewsBatcher.flush();
        pinBatcher.flush();
    }

    public void resetPins() throws SQLException {
        Map<String, List<MappedLayer>> layers = layersMapper.getLayers();
        for (List<MappedLayer> mappedLayers : layers.values()) {
            for (MappedLayer mappedLayer : mappedLayers) {
                resetPin(mappedLayer);
            }
        }
    }

    public void addViewAndPin(ViewRecord view) throws Exception {
        logger.log(Level.FINE, "Pinning layers for view: " + view.getId());
        logger.log(Level.FINER, view);
        updateLayersPins(view.getLayers(), view.getTimeOriginal(), view.getTimeMain(), true);
        insertView(view);
    }

    public void disableAndUnpin(Long viewId) throws Exception {
        ViewRecord view = fetchView(viewId);
        updateLayersPins(view.getLayers(), view.getTimeOriginal(), view.getTimeMain(), false);
        deleteView(viewId);
    }

    public boolean truncateViews() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            logger.log(Level.INFO, "Truncating views table");
            stmt.executeUpdate(TRUNCATE_TABLE_VIEWS);
            return true;
        }
    }

    public ViewRecord buildView(ParsedView view) {
        ViewRecord viewRecord =
                new ViewRecord(
                        view.getViewId(),
                        view.getTime(),
                        view.getTime(),
                        view.getLayers(),
                        view.getLastUpdate());
        return viewRecord;
    }

    public void updateLastUpdate(Instant lastUpdate) throws SQLException {
        try (PreparedStatement prepStmt = connection.prepareStatement(UPDATE_LAST_RUN_QUERY)) {
            prepStmt.setTimestamp(1, Timestamp.from(lastUpdate), utcCalendar);
            prepStmt.executeUpdate();
        }
    }

    public Timestamp retrieveLastUpdate() throws SQLException {
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(GET_LAST_RUN_QUERY)) {
            if (rs.next()) {
                return rs.getTimestamp(1, utcCalendar);
            } else {
                return null;
            }
        }
    }

    private void updateLayersPins(
            Collection<String> layers, Instant originalTime, Instant mainTime, boolean addPin)
            throws SQLException, IOException {
        for (String layer : layers) {
            pinGeoserverLayer(layer, originalTime, mainTime, addPin);
        }
    }

    public void syncView(ViewRecord view, ParsedView parsed) throws SQLException, IOException {
        Instant originalTime = view.getTimeOriginal();
        Instant viewTime = parsed.getTime();
        if (viewTime.equals(view.getTimeMain())) {
            logger.log(Level.FINE, "View time has not been modified");
            redoView(view, parsed);
        } else if (!isExtendingWindow(originalTime, viewTime)) {
            logger.log(Level.FINE, "View time has been updated outside the pinning window");
            redoView(view, parsed);
        } else {
            logger.log(Level.FINE, "View time has been updated within the pinning window");
            extendView(view, parsed);
        }
        ViewRecord record = buildView(parsed);
        updateView(record);
    }

    public ViewRecord fetchView(Long viewId) throws Exception {
        logger.log(Level.FINE, "Retrieving view: " + viewId);
        try (PreparedStatement prepStmt = connection.prepareStatement(GET_VIEW_QUERY)) {
            prepStmt.setLong(1, viewId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {

                    Instant timeOriginal =
                            rs.getTimestamp("time_original", utcCalendar).toInstant();
                    Instant timeMain = rs.getTimestamp("time_main", utcCalendar).toInstant();
                    long id = rs.getLong("view_id");

                    Array layersArray = rs.getArray("layers_list");
                    String[] layers = (String[]) layersArray.getArray();
                    List<String> layersList = Arrays.asList(layers);

                    Instant lastUpdated = rs.getTimestamp("last_update", utcCalendar).toInstant();

                    ViewRecord record =
                            new ViewRecord(id, timeOriginal, timeMain, layersList, lastUpdated);
                    logger.log(Level.FINER, record);
                    return record;
                }
            }
        }
        return null;
    }

    private void setPinLayer(Instant originalTime, Instant mainTime, MappedLayer layer, boolean add)
            throws SQLException, IOException {

        boolean mainGreaterThanOriginal = isGreaterThan(mainTime, originalTime);
        Instant minTime = mainGreaterThanOriginal ? originalTime : mainTime;
        Instant maxTime = mainGreaterThanOriginal ? mainTime : originalTime;
        Instant start = layer.getNearest(getLeft(minTime));
        Instant end = layer.getNearest(getRight(maxTime));
        setPinLayerRange(start, end, layer, add);
    }

    private void setPinLayerRange(Instant start, Instant end, MappedLayer layer, boolean add)
            throws SQLException {
        logger.log(
                Level.FINE,
                String.format(
                        "%sinning layer %s in range (%s,%s)",
                        add ? "P" : "Unp", layer.getGeoServerLayerIdentifier(), start, end));
        String updateQuery =
                pinBatcher.update(
                        layer.getTableName(), layer.getTemporalAttribute(), start, end, add);
        logger.log(Level.FINEST, "pinning query:" + updateQuery);
    }

    private boolean isGreaterThan(Instant mainTime, Instant originalTime) {
        return originalTime.getEpochSecond() <= mainTime.getEpochSecond();
    }

    private Instant getLeft(Instant time) {
        return time.minus(duration);
    }

    private Instant getRight(Instant time) {
        return time.plus(duration);
    }

    private void insertView(ViewRecord view) throws SQLException {
        logger.log(Level.FINE, "Inserting the view in the views table: " + view.getId());
        String insertQuery = viewsBatcher.insertView(view);
        logger.log(Level.FINEST, "Inserting view query: " + insertQuery);
    }

    private void updateView(ViewRecord view) throws SQLException {
        logger.log(Level.FINE, "Updating the view in the views table: " + view.getId());
        String updateQuery = viewsBatcher.updateView(view);
        logger.log(Level.FINEST, "Updating view query: " + updateQuery);
    }

    private void deleteView(Long viewId) throws Exception {
        logger.log(Level.FINE, "Deleting the view from the views table: " + viewId);
        String deleteQuery = viewsBatcher.deleteView(viewId);
        logger.log(Level.FINEST, "Deleting view query: " + deleteQuery);
    }

    private void resetPin(MappedLayer layer) throws SQLException {
        String tableName = layer.getTableName();
        logger.log(Level.FINE, String.format("Resetting pin for table %s", tableName));
        String resetQuery = pinBatcher.resetPins(tableName);
        logger.log(Level.FINEST, "Resetting pin query: " + resetQuery);
    }

    private void pinGeoserverLayer(
            String layer, Instant originalTime, Instant mainTime, boolean addPin)
            throws SQLException, IOException {
        List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
        for (MappedLayer mappedLayer : mappedLayers) {
            logger.log(Level.FINER, String.format("Updating pins for layer %s:", mappedLayer));
            setPinLayer(originalTime, mainTime, mappedLayer, addPin);
        }
    }

    private void redoView(ViewRecord view, ParsedView parsed) throws SQLException, IOException {
        logger.log(
                Level.FINE,
                String.format(
                        "Updating the view %d: stored view originalTime: %s, updating view time: %s",
                        view.getId(), view.getTimeOriginal(), parsed.getTime()));
        LayersUpdate layersUpdate = new LayersUpdate(view.getLayers(), parsed.getLayers());

        // Unpin removed layers
        updateLayersPins(
                layersUpdate.getRemoved(), view.getTimeOriginal(), view.getTimeMain(), false);

        // Pin newly added layers
        ViewRecord updatedView = buildView(parsed);
        updateLayersPins(layersUpdate.getAdded(), parsed.getTime(), parsed.getTime(), false);

        // recompute pinning for existing layers
        if (!updatedView.getTimeMain().equals(view.getTimeMain())
                && !updatedView.getTimeOriginal().equals(view.getTimeOriginal())) {
            updateLayersPins(
                    layersUpdate.getUnchanged(), view.getTimeOriginal(), view.getTimeMain(), false);
            updateLayersPins(
                    layersUpdate.getUnchanged(),
                    updatedView.getTimeOriginal(),
                    updatedView.getTimeMain(),
                    true);
        }
    }

    private void extendView(ViewRecord view, ParsedView parsed) throws SQLException, IOException {
        logger.log(
                Level.FINE,
                String.format(
                        "Updating the view %d: stored view originalTime: %s, updating view time: %s",
                        view.getId(), view.getTimeOriginal(), parsed.getTime()));
        LayersUpdate layersUpdate = new LayersUpdate(view.getLayers(), parsed.getLayers());

        // Unpin removed layers
        updateLayersPins(
                layersUpdate.getRemoved(), view.getTimeOriginal(), view.getTimeMain(), false);

        // Pin newly added layers
        updateLayersPins(layersUpdate.getAdded(), view.getTimeOriginal(), parsed.getTime(), false);

        for (String layer : layersUpdate.getUnchanged()) {
            List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
            for (MappedLayer mappedLayer : mappedLayers) {
                Instant previousOriginalTime = view.getTimeOriginal();
                Instant previousMainTime = view.getTimeMain();
                Instant previousOrigLeft = mappedLayer.getNearest(getLeft(previousOriginalTime));
                Instant previousOrigRight = mappedLayer.getNearest(getRight(previousOriginalTime));
                Instant previousMainLeft = mappedLayer.getNearest(getLeft(previousMainTime));
                Instant previousMainRight = mappedLayer.getNearest(getRight(previousMainTime));
                Instant newLeft = mappedLayer.getNearest(getLeft(parsed.getTime()));
                Instant newRight = mappedLayer.getNearest(getRight(parsed.getTime()));

                // extend to right
                if (newLeft.equals(previousMainLeft) && newRight.equals(previousMainRight)) {
                    logger.log(
                            Level.FINE,
                            "The extended view covers same records as before. No action needed on layer: "
                                    + mappedLayer.getLayerName());
                    continue;
                }
                if (isGreaterThan(newRight, previousOrigRight)
                        && isGreaterThan(previousOrigRight, newLeft)) {
                    if (isGreaterThan(newRight, previousMainRight)) {
                        setPinLayerRange(
                                previousMainRight.plus(Duration.ofSeconds(1)),
                                newRight,
                                mappedLayer,
                                true);
                    } else if (isGreaterThan(newLeft, previousMainRight)) {
                        setPinLayerRange(
                                previousOrigRight.plus(Duration.ofSeconds(1)),
                                newRight,
                                mappedLayer,
                                true);
                        setPinLayerRange(
                                previousMainLeft,
                                previousOrigLeft.minus(Duration.ofSeconds(1)),
                                mappedLayer,
                                false);
                    } else {
                        System.out.println("WHAT TO DO HERE");
                    }

                } else if (isGreaterThan(previousOrigLeft, newLeft)
                        && isGreaterThan(newRight, previousOrigLeft)) {
                    if (isGreaterThan(previousMainLeft, newRight)) {
                        setPinLayerRange(
                                newLeft,
                                previousOrigLeft.minus(Duration.ofSeconds(1)),
                                mappedLayer,
                                true);
                        setPinLayerRange(
                                previousMainRight.plus(Duration.ofSeconds(1)),
                                previousMainRight,
                                mappedLayer,
                                false);

                    } else if (isGreaterThan(newRight, previousMainLeft)) {
                        setPinLayerRange(
                                newLeft,
                                previousMainLeft.minus(Duration.ofSeconds(1)),
                                mappedLayer,
                                true);
                    } else {
                        System.out.println("WHAT TO DO HERE");
                    }
                }
            }
        }
    }

    private boolean isExtendingWindow(Instant originalTime, Instant viewTime) {

        Instant originalStart = originalTime.minus(duration);
        Instant originalEnd = originalTime.plus(duration);

        // Expand viewTime window
        Instant viewStart = viewTime.minus(duration);
        Instant viewEnd = viewTime.plus(duration);

        return originalStart.isBefore(viewEnd) && viewStart.isBefore(originalEnd);
    }
}
