package org.geoserver.eumetsat.pinning.views;

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
import org.geoserver.eumetsat.pinning.LayersMapper;
import org.geoserver.eumetsat.pinning.MappedLayer;
import org.geoserver.eumetsat.pinning.PinningServiceLogger;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
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
                logger.log(Level.FINER, "The following layers have been removed in the updated view: " + String.join(",", removed));
            }

            // added layers (present in layersAfter but not in layersBefore)
            added = new HashSet<>(layersAfter);
            added.removeAll(layersBefore);
            if (!added.isEmpty()) {
                logger.log(Level.FINER, "The following layers have been added in the updated view: " + String.join(",", added));
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

    private static final String RESET_PINS_QUERY = "UPDATE %s SET pin=0 WHERE pin != 0;";

    private static final String GET_VIEW_QUERY =
            "SELECT * FROM " + VIEWS_TABLE + " WHERE view_id = ?";

    private static final String UPDATE_PINS_QUERY =
            "UPDATE %s SET pin = pin %s 1 WHERE %s >= '%s' and %s <= '%s';";

    private static final String ADD_VIEW_QUERY =
            "INSERT INTO "
                    + VIEWS_TABLE
                    + " (time_original, time_main, view_id, layers_list, last_update) VALUES (?, ?, ?, ?, ?)";

    private static final String UPDATE_VIEW_QUERY =
            "UPDATE "
                    + VIEWS_TABLE
                    + " SET time_main = ?, layers_list = ?, last_update = ? WHERE view_id = ?";

    private static final String DELETE_VIEW_QUERY =
            "DELETE FROM " + VIEWS_TABLE + " WHERE view_id = ?";

    private static final String GET_LAST_RUN_QUERY = "SELECT last_run from " + RUNS_TABLE;

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

    private int count;

    private Connection connection;

    private Duration duration;

    @PostConstruct
    private void initDuration() {
        // Splitting the provided pinning minutes in half (which means multiplying minutes by 30 seconds)
        duration = Duration.ofSeconds(config.pinningMinutes()*30);
    }

    public void init(Connection connection) {
        this.connection = connection;
        count = 0;
    }

    public void flushBatch(Statement statement) throws SQLException {
        if (count > 0) {
            statement.executeBatch();
            count = 0;
        }
    }

    public void resetPins(Statement statement) throws SQLException {
        Map<String, List<MappedLayer>> layers = layersMapper.getLayers();
        for (List<MappedLayer> mappedLayers : layers.values()) {
            for (MappedLayer mappedLayer : mappedLayers) {
                resetPin(mappedLayer, statement);
            }
        }
    }

    public void addViewAndPin(ViewRecord view, Statement stmt) throws Exception {
        insertView(view);
        logger.log(Level.FINE, "Pinning layers for view: " + view.getId());
        logger.log(Level.FINER, view);
        updateLayersPins(stmt, view.getLayers(), view.getTimeOriginal(), view.getTimeMain(), true);
    }

    public void disableAndUnpin(Long viewId, Statement statement) throws Exception {
        ViewRecord view = fetchView(viewId);
        updateLayersPins(
                statement, view.getLayers(), view.getTimeOriginal(), view.getTimeMain(), false);
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
                return rs.getTimestamp(1);
            } else {
                return null;
            }
        }
    }

    private void updateLayersPins(
            Statement statement,
            Collection<String> layers,
            Instant originalTime,
            Instant mainTime,
            boolean addPin)
            throws SQLException, IOException {
        for (String layer : layers) {
            pinGeoserverLayer(statement, layer, originalTime, mainTime, addPin);
        }
    }

    public void syncView(ViewRecord view, ParsedView parsed, Statement statement)
            throws SQLException, IOException {
        Instant originalTime = view.getTimeOriginal();
        Instant viewTime = parsed.getTime();
        if (viewTime.equals(view.getTimeMain()) || !isExtendingWindow(originalTime, viewTime)) {
            redoView(view, parsed, statement);
        } else {
            extendView(view, parsed, statement);
        }
        ViewRecord record = buildView(parsed);
        updateViewQuery(record);
    }

    private void updateViewQuery(ViewRecord view) throws SQLException {
        logger.log(Level.FINE, "Updating view: " + view.getId());
        try (PreparedStatement prepStmt = connection.prepareStatement(UPDATE_VIEW_QUERY)) {
            Timestamp timeMain = Timestamp.from(view.getTimeMain());
            Timestamp lastUpdate = Timestamp.from(view.getLastUpdate());
            Array layersArray = connection.createArrayOf("text", view.getLayers().toArray());
            prepStmt.setTimestamp(1, timeMain, utcCalendar);
            prepStmt.setArray(2, layersArray);
            prepStmt.setTimestamp(3, lastUpdate, utcCalendar);
            prepStmt.setLong(4, view.getId());
            prepStmt.executeUpdate();
            logger.log(Level.FINEST, "updating view query: " + buildUpdateViewQuery(timeMain, layersArray,lastUpdate, view.getId()));
        }
    }

    private String buildUpdateViewQuery(Timestamp timeMain, Array layersArray, Timestamp lastUpdate, long id) {
        return "UPDATE " + VIEWS_TABLE + " SET time_main = '" + timeMain +
                "', layers = '" + layersArray +
                "', last_update = '" + lastUpdate +
                "' WHERE id = " + id + ";";

    }

    public ViewRecord fetchView(Long viewId) throws Exception {
        logger.log(Level.FINE, "Retrieving view: " + viewId);
        try (PreparedStatement prepStmt = connection.prepareStatement(GET_VIEW_QUERY)) {
            prepStmt.setLong(1, viewId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {

                    Instant timeOriginal = rs.getTimestamp("time_original", utcCalendar).toInstant();
                    Instant timeMain = rs.getTimestamp("time_main", utcCalendar).toInstant();
                    long id = rs.getLong("view_id");

                    Array layersArray = rs.getArray("layers_list");
                    String[] layers = (String[]) layersArray.getArray();
                    List<String> layersList = Arrays.asList(layers);

                    Instant lastUpdated = rs.getTimestamp("last_update", utcCalendar).toInstant();

                    ViewRecord record = new ViewRecord(id, timeOriginal, timeMain, layersList, lastUpdated);
                    logger.log(Level.FINER, record);
                    return record;
                }
            }
        }
        return null;
    }

    private void deleteView(Long viewId) throws Exception {
        logger.log(Level.FINE, "Deleting view: " + viewId);
        try (PreparedStatement prepStmt = connection.prepareStatement(DELETE_VIEW_QUERY)) {
            prepStmt.setLong(1, viewId);
            prepStmt.executeUpdate();
        }
    }

    private void setPinLayer(
            Statement statement,
            Instant originalTime,
            Instant mainTime,
            MappedLayer layer,
            boolean add)
            throws SQLException, IOException {

        boolean mainGreaterThanOriginal = isGreaterThan(mainTime, originalTime);
        Instant minTime = mainGreaterThanOriginal ? originalTime : mainTime;
        Instant maxTime = mainGreaterThanOriginal ? mainTime : originalTime;
        Instant start = layer.getNearest(getLeft(minTime));
        Instant end = layer.getNearest(getRight(maxTime));
        setPinLayerRange(statement, start, end, layer, add);
    }

    private void setPinLayerRange(
            Statement statement, Instant start, Instant end, MappedLayer layer, boolean add)
            throws SQLException {
        String updateQuery =
                String.format(
                        UPDATE_PINS_QUERY,
                        layer.getTableName(),
                        add ? "+" : "-",
                        layer.getTemporalAttribute(),
                        start,
                        layer.getTemporalAttribute(),
                        end);
        logger.log(
                Level.FINE,
                String.format(
                        "%spinning layer %s in range (%s,%s)",
                        add ? "" : "un", layer.getGeoServerLayerIdentifier(), start, end));
        logger.log(Level.FINEST, "pinning query:" + updateQuery);
        addBatch(statement, updateQuery);

    }

    private void addBatch(Statement statement, String query) throws SQLException {
        statement.addBatch(query);
        if (count++ == config.batchSize()) {
            statement.executeBatch();
            count = 0;
        }
    }

    private boolean isGreaterThan(Instant mainTime, Instant originalTime) {
        return originalTime.getEpochSecond() < mainTime.getEpochSecond();
    }

    private Instant getLeft(Instant time) {
        return time.minus(duration);
    }

    private Instant getRight(Instant time) {
        return time.plus(duration);
    }

    private void insertView(ViewRecord view) throws SQLException {
        logger.log(Level.FINE, "Inserting view: " + view.getId());
        try (PreparedStatement prepStmt = connection.prepareStatement(ADD_VIEW_QUERY)) {
            prepStmt.setTimestamp(1, Timestamp.from(view.getTimeOriginal()), utcCalendar);
            prepStmt.setTimestamp(2, Timestamp.from(view.getTimeMain()), utcCalendar);
            prepStmt.setLong(3, view.getId());
            prepStmt.setArray(4, connection.createArrayOf("text", view.getLayers().toArray()));
            prepStmt.setTimestamp(5, Timestamp.from(view.getLastUpdate()), utcCalendar);
            prepStmt.executeUpdate();
        }
    }

    private void resetPin(MappedLayer layer, Statement statement) throws SQLException {
        String tableName = layer.getTableName();
        String resetSql = RESET_PINS_QUERY.replace("%s", tableName);
        logger.log(Level.FINE, String.format("Resetting pin for table %s", tableName));
        addBatch(statement, resetSql);
    }

    private void pinGeoserverLayer(
            Statement statement,
            String layer,
            Instant originalTime,
            Instant mainTime,
            boolean addPin)
            throws SQLException, IOException {
        List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
        for (MappedLayer mappedLayer : mappedLayers) {
            logger.log(Level.FINER, String.format("Updating pins for layer %s:", mappedLayer));
            setPinLayer(statement, originalTime, mainTime, mappedLayer, addPin);
        }
    }

    private void redoView(ViewRecord view, ParsedView parsed, Statement statement)
            throws SQLException, IOException {
        logger.log(Level.FINE, "Updating the view: " + view.getId());
        LayersUpdate layersUpdate = new LayersUpdate(view.getLayers(), parsed.getLayers());

        // Unpin removed layers
        updateLayersPins(
                statement,
                layersUpdate.getRemoved(),
                view.getTimeOriginal(),
                view.getTimeMain(),
                false);

        // Pin newly added layers
        ViewRecord updatedView = buildView(parsed);
        updateLayersPins(
                statement, layersUpdate.getAdded(), parsed.getTime(), parsed.getTime(), false);

        // recompute pinning for existing layers
        if (!updatedView.getTimeMain().equals(view.getTimeMain()) && !updatedView.getTimeOriginal().equals(view.getTimeOriginal())) {
            updateLayersPins(
                    statement,
                    layersUpdate.getUnchanged(),
                    view.getTimeOriginal(),
                    view.getTimeMain(),
                    false);
            updateLayersPins(
                    statement,
                    layersUpdate.getUnchanged(),
                    updatedView.getTimeOriginal(),
                    updatedView.getTimeMain(),
                    true);
        }
    }

    private void extendView(ViewRecord view, ParsedView parsed, Statement statement)
            throws SQLException, IOException {
        LayersUpdate layersUpdate = new LayersUpdate(view.getLayers(), parsed.getLayers());

        // Unpin removed layers
        updateLayersPins(
                statement,
                layersUpdate.getRemoved(),
                view.getTimeOriginal(),
                view.getTimeMain(),
                false);

        // Pin newly added layers
        updateLayersPins(
                statement,
                layersUpdate.getAdded(),
                view.getTimeOriginal(),
                parsed.getTime(),
                false);

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
                if (isGreaterThan(newRight, previousOrigRight)
                        && isGreaterThan(previousOrigRight, newLeft)) {
                    if (isGreaterThan(newRight, previousMainRight)) {
                        setPinLayerRange(
                                statement,
                                previousMainRight.plus(Duration.ofSeconds(1)),
                                newRight,
                                mappedLayer,
                                true);
                    } else if (isGreaterThan(newLeft, previousMainRight)) {
                        setPinLayerRange(
                                statement,
                                previousOrigRight.plus(Duration.ofSeconds(1)),
                                newRight,
                                mappedLayer,
                                true);
                        setPinLayerRange(
                                statement,
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
                                statement,
                                newLeft,
                                previousOrigLeft.minus(Duration.ofSeconds(1)),
                                mappedLayer,
                                true);
                        setPinLayerRange(
                                statement,
                                previousMainRight.plus(Duration.ofSeconds(1)),
                                previousMainRight,
                                mappedLayer,
                                false);

                    } else if (isGreaterThan(newRight, previousMainLeft)) {
                        setPinLayerRange(
                                statement,
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
