package org.geoserver.eumetsat.pinning.views;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import org.geoserver.eumetsat.pinning.LayersMapper;
import org.geoserver.eumetsat.pinning.MappedLayer;
import org.geoserver.eumetsat.pinning.PinningServiceLogger;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ViewsEvaluator {

    public static final String VIEWS_TABLE = "pinning.views";

    private static final String TRUNCATE_TABLE_VIEWS = "TRUNCATE table " + VIEWS_TABLE;

    private static final String RESET_PINS_QUERY = "UPDATE %s SET pin=0 WHERE pin > 0;";

    private static final String ADD_VIEW_QUERY =
            "INSERT INTO "
                    + VIEWS_TABLE
                    + " (time_original, time_main, view_id, layers_list, last_updated) VALUES (?, ?, ?, ?, ?)";

    private static final String DELETE_VIEW_QUERY =
            "DELETE FROM " + VIEWS_TABLE + " WHERE view_id = ?";

    private static final String GET_VIEW_QUERY =
            "SELECT * FROM " + VIEWS_TABLE + " WHERE view_id = ?";

    private static final String UPDATE_PINS_QUERY =
            "UPDATE %s SET pin = pin %s 1 WHERE %s between '%s' and '%s';";

    private static final String GET_LAST_UPDATE_QUERY =
            "SELECT max(last_updated) from " + VIEWS_TABLE;

    @Autowired private LayersMapper layersMapper;

    @Autowired private PinningServiceConfig config;

    @Autowired private PinningServiceLogger logger;

    public int getCount() {
        return count;
    }

    private int count;

    private Connection connection;

    public void reset(Connection connection) {
        this.connection = connection;
        count = 0;
    }

    public void addPins(ViewRecord view, Statement statement) throws SQLException {
        for (String layer : view.getLayers()) {
            pinGeoserverLayer(statement, view, layer, true);
        }
    }

    private void setPinLayer(
            Statement statement,
            Instant originalTime,
            Instant mainTime,
            MappedLayer layer,
            boolean add)
            throws SQLException {

        Instant minTime =
                originalTime.getEpochSecond() < mainTime.getEpochSecond() ? originalTime : mainTime;
        Instant maxTime =
                originalTime.getEpochSecond() > mainTime.getEpochSecond() ? originalTime : mainTime;
        Instant start = minTime.minus(config.pinningMinutes(), ChronoUnit.MINUTES);
        Instant end = maxTime.plus(config.pinningMinutes(), ChronoUnit.MINUTES);
        String updateQuery =
                String.format(
                        UPDATE_PINS_QUERY,
                        layer.getTableName(),
                        add ? "+" : "-",
                        layer.getTemporalAttribute(),
                        start,
                        end);
        logger.log(
                Level.FINE,
                String.format(
                        "%spinning layer %s in range (%s,%s)",
                        add ? "" : "un", layer.getGeoServerLayerIdentifier(), start, end));
        statement.addBatch(updateQuery);
    }

    public void finalizeBatch(Statement statement) throws SQLException {
        if (count > 0) {
            statement.executeBatch();
            count = 0;
        }
    }

    public ViewRecord addView(ParsedView view) throws Exception {
        Long viewId = view.getViewId();
        String time = view.getTime();
        List<String> layers = view.getLayers();
        logger.log(Level.FINE, "Inserting view: " + viewId);
        try (PreparedStatement prepStmt = connection.prepareStatement(ADD_VIEW_QUERY)) {
            Instant instant = Instant.parse(time);
            Instant now = Instant.now();
            Timestamp timestamp = Timestamp.from(instant);
            prepStmt.setTimestamp(1, timestamp);
            prepStmt.setTimestamp(2, timestamp);
            prepStmt.setLong(3, viewId);
            prepStmt.setArray(4, connection.createArrayOf("text", layers.toArray()));
            prepStmt.setTimestamp(5, Timestamp.from(now));
            prepStmt.executeUpdate();
            return new ViewRecord(instant, instant, viewId, layers, now);
        }
    }

    public ViewRecord getView(Long viewId) throws Exception {
        logger.log(Level.FINE, "Retrieving view: " + viewId);
        try (PreparedStatement prepStmt = connection.prepareStatement(GET_VIEW_QUERY)) {
            prepStmt.setLong(1, viewId);
            try (ResultSet rs = prepStmt.executeQuery()) {
                if (rs.next()) {

                    Instant timeOriginal = rs.getTimestamp("time_original").toInstant();
                    Instant timeMain = rs.getTimestamp("time_main").toInstant();
                    long id = rs.getLong("view_id");

                    Array layersArray = rs.getArray("layers_list");
                    String[] layers = (String[]) layersArray.getArray();
                    List<String> layersList = Arrays.asList(layers);

                    Instant lastUpdated = rs.getTimestamp("last_updated").toInstant();

                    return new ViewRecord(timeOriginal, timeMain, id, layersList, lastUpdated);
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

    public void resetPins() throws SQLException {
        Map<String, List<MappedLayer>> layers = layersMapper.getLayers();
        for (List<MappedLayer> mappedLayers : layers.values()) {
            for (MappedLayer mappedLayer : mappedLayers) {
                resetPin(mappedLayer);
            }
        }
    }

    private void resetPin(MappedLayer layer) throws SQLException {
        String tableName = layer.getTableName();
        String resetSql = RESET_PINS_QUERY.replace("%s", tableName);
        try (PreparedStatement prepStmt = connection.prepareStatement(resetSql)) {
            logger.log(Level.FINE, String.format("Resetting pin for table %s", tableName));
            int result = prepStmt.executeUpdate();
            if (result >= 0) {
                logger.log(
                        Level.FINER,
                        String.format(
                                "Resetting pin for table %s affected %d rows", tableName, result));
            }
        }
    }

    public boolean resetViews() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            logger.log(Level.INFO, "Truncating views table");
            stmt.executeUpdate(TRUNCATE_TABLE_VIEWS);
            return true;
        }
    }

    public Timestamp retrieveLastUpdate() throws SQLException {
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(GET_LAST_UPDATE_QUERY)) {
            if (rs.next()) {
                return rs.getTimestamp(1);
            } else {
                return Timestamp.from(Instant.now());
            }
        }
    }

    public void disableAndUnpin(Long viewId, Statement statement) throws Exception {
        ViewRecord view = getView(viewId);
        for (String layer : view.getLayers()) {
            pinGeoserverLayer(statement, view, layer, false);
        }
        deleteView(viewId);
    }

    private void pinGeoserverLayer(Statement statement, ViewRecord view, String layer, boolean add)
            throws SQLException {
        List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
        if (mappedLayers == null || mappedLayers.isEmpty()) {
            throw new IllegalArgumentException(
                    "The following layer has no associated mapping. Aborting pinning " + layer);
        }
        for (MappedLayer mappedLayer : mappedLayers) {
            logger.log(Level.FINER, String.format("Pinning layer %s:", mappedLayer));
            setPinLayer(statement, view.getTimeOriginal(), view.getTimeMain(), mappedLayer, add);
            count++;
            if (count == config.batchSize()) {
                statement.executeBatch();
                count = 0;
            }
        }
    }

    public void updateView(ViewRecord view, ParsedView parsed, Statement statement)
            throws SQLException {
        HashSet<String> previousLayers = new HashSet<>(view.getLayers());
        HashSet<String> updatedLayers = new HashSet<>(parsed.getLayers());

        HashSet<String> removed = new HashSet<>(previousLayers);
        removed.removeAll(updatedLayers);

        HashSet<String> added = new HashSet<>(updatedLayers);
        removed.removeAll(previousLayers);

        HashSet<String> common = new HashSet<>(previousLayers);
        removed.retainAll(updatedLayers);

        // Unpin removed layers
        for (String layer : removed) {
            pinGeoserverLayer(statement, view, layer, false);
        }
        // Pin newly added layers
        for (String layer : added) {
            pinGeoserverLayer(statement, view, layer, true);
        }

        // TODO: updating
    }
}
