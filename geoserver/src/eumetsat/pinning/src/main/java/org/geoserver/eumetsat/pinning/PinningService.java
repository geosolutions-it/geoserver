package org.geoserver.eumetsat.pinning;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.geoserver.eumetsat.pinning.rest.PinningServiceController;
import org.geoserver.eumetsat.pinning.views.ParsedView;
import org.geoserver.eumetsat.pinning.views.TestContext;
import org.geoserver.eumetsat.pinning.views.View;
import org.geoserver.eumetsat.pinning.views.ViewsClient;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PinningService {

    private static final String VIEWS_TABLE = "pinning.views";
    private static final String TRUNCATE_TABLE_VIEWS = "TRUNCATE table " + VIEWS_TABLE;
    private static final String RESET_PINS_SQL = "UPDATE %s SET pin=0 WHERE pin > 0;";
    private static final String UPDATE_PINS_SQL = "UPDATE %s SET pin = pin %s 1 WHERE %s between '%s' and '%s';";

    private static final String GET_LAST_UPDATE = "SELECT max(last_updated) from " + VIEWS_TABLE;
    //TODO: get schema/table from configuration
    private static final String ADD_VIEW_SQL = "INSERT INTO " + VIEWS_TABLE + " (time_original, time_main, view_id, layers_list, last_updated) VALUES (?, ?, ?, ?, ?)";

    private final ExecutorService executorService =
            Executors.newSingleThreadExecutor(); // Only 1 task at a time
    private final AtomicReference<UUID> currentTaskId = new AtomicReference<>(null);
    private final AtomicReference<String> taskStatus = new AtomicReference<>("IDLE");
    private static final int PINNING_LOCK_ID = 9111119;
    private static final Logger LOGGER = Logging.getLogger(PinningServiceController.class);

    @Autowired private Catalog catalog;

    @Autowired private PinningServiceConfig config;

    @Autowired private ViewsClient viewsClient;

    @Autowired private LayersMapper layersMapper;

    public Optional<UUID> reset() throws SQLException {
        UUID taskId = null;
        Connection conn = config.dataSource().getConnection();
        conn.setAutoCommit(false);
        try {
            if (!acquireLock(conn)) {
                LOGGER.warning("Another process is holding the lock");
                return Optional.empty();
            }
            taskId = UUID.randomUUID();

            currentTaskId.set(taskId);
            taskStatus.set("RUNNING");

            CompletableFuture.runAsync(
                    () -> {
                        try {
                            log(Level.INFO, "Global RESET Started");
                            resetPinning(conn);
                            conn.commit();
                            taskStatus.set("COMPLETED");
                            log(Level.INFO, "Global RESET Completed");
                        } catch (Exception e) {
                            processFailure("Global RESET", conn, e);
                        } finally {
                            releaseResources(conn);
                        }
                    });
        } catch (SQLException e) {
            LOGGER.severe("An exception occurred: " + e);
        }

        return Optional.of(taskId);
    }

    public Optional<UUID> incremental() throws SQLException {
        UUID taskId = null;
        Connection conn = config.dataSource().getConnection();
        conn.setAutoCommit(false);
        try {
            if (!acquireLock(conn)) {
                LOGGER.warning("Another process is holding the lock");
                return Optional.empty();
            }
            taskId = UUID.randomUUID();

            currentTaskId.set(taskId);
            taskStatus.set("RUNNING");

            CompletableFuture.runAsync(
                    () -> {
                        try {
                            log(Level.INFO, "Incremental pinning Started");
                            incrementalPinning(conn);
                            conn.commit();
                            taskStatus.set("COMPLETED");
                            log(Level.INFO, "Incremental pinning Completed");
                        } catch (Exception e) {
                            processFailure("Incremental Pinning", conn, e);
                        } finally {
                            releaseResources(conn);
                        }
                    });
        } catch (SQLException e) {
            LOGGER.severe("An exception occurred: " + e);
        }

        return Optional.of(taskId);
    }

    private void processFailure(String pinningType, Connection conn, Exception e) {
        taskStatus.set("FAILED");
        String message = pinningType + " Aborted due to an Exception:" + e.getLocalizedMessage();
        log(Level.SEVERE, message);
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                log(Level.SEVERE, "Exception occurred while rolling-back: " + ex.getLocalizedMessage());
            }
        }
        throw new RuntimeException(message, e);
    }

    private void releaseResources(Connection conn) {
        releaseLock(conn);
        try {
            if (conn != null) {
                conn.close();
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        currentTaskId.set(null);
    }

    private void incrementalPinning(Connection conn) throws Exception {
        log(Level.FINE,"Retrieved last updated time");
        Timestamp timestamp = null;
        // TODO: This section is only for testing.
        String timestampString = TestContext.getUpdateTime();
        if (timestampString!= null) {
            TestContext.clear();

        } else {
            try (Statement statement = conn.createStatement();
                ResultSet rs = statement.executeQuery(GET_LAST_UPDATE)) {
                if (rs.next()) {
                    timestamp = rs.getTimestamp(1);
                } else {
                    timestamp = Timestamp.from(Instant.now());
                }
            }
            timestampString = timestamp.toInstant()
                        .atOffset(ZoneOffset.UTC)
                        .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

        }
        log(Level.FINE, "Fetching views with lastUpdated > " + timestampString);
        List<View> views = viewsClient.fetchViews(timestampString);
        log(Level.INFO,
                String.format(
                        "Retrieved %d views from the preferences endpoint", views.size()));

        log(Level.INFO, "Inserting views");
        int count = 0;
        try (Statement statement = conn.createStatement()) {
            for (View view: views) {
                ParsedView parsed = viewsClient.parseView(view);
                Long viewId = parsed.getViewId();
                String mode = parsed.getTimeMode();
                if (!"absolute".equalsIgnoreCase(mode)) {
                    // Only event views are pinned.
                    // Live views (with mode=latest) don't need that
                    log(Level.FINE,String.format("View with id=%s will be skipped since it's a live view", viewId));
                    continue;
                }
                String time = parsed.getTime();
                List<String> layers = parsed.getLayers();
                log(Level.FINER, "Inserting view" + viewId);
                addView(conn, viewId, time, layers);

                for (String layer : layers) {
                    List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
                    if (mappedLayers == null || mappedLayers.isEmpty()) {
                        throw new IllegalArgumentException("The following layer has no associated mapping. Aborting pinning " + layer);
                    }
                    for (MappedLayer mappedLayer : mappedLayers) {
                        log(Level.FINER, String.format("Pinning layer %s:", mappedLayer));
                        pinLayer(statement, time, mappedLayer);
                        count++;
                        //TODO: configure batchsize
                        if (count == 10) {
                            statement.executeBatch();
                            count = 0;
                        }
                    }
                }
            }
            if (count > 0) {
                statement.executeBatch();
            }
        }
    }

    private void resetPinning(Connection conn) throws Exception {
        log(Level.FINE, "Resetting the pins");
        resetPins(conn);

        log(Level.FINE, "Resetting the views");
        resetView(conn);

        List<View> views = viewsClient.fetchViews(null);
        log(Level.INFO,
                    String.format(
                            "Retrieved %d views from the preferences endpoint", views.size()));

        log(Level.INFO, "Inserting views");
        int count = 0;
        try (Statement statement = conn.createStatement()) {
            for (View view: views) {
                ParsedView parsed = viewsClient.parseView(view);
                Long viewId = parsed.getViewId();
                String mode = parsed.getTimeMode();
                if (!"absolute".equalsIgnoreCase(mode)) {
                    // Only event views are pinned.
                    // Live views (with mode=latest) don't need that
                    log(Level.FINE,String.format("View with id=%s will be skipped since it's a live view", viewId));
                    continue;
                }
                String time = parsed.getTime();
                List<String> layers = parsed.getLayers();
                log(Level.FINER, "Inserting view" + viewId);
                addView(conn, viewId, time, layers);

                for (String layer : layers) {
                    List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
                    if (mappedLayers == null || mappedLayers.isEmpty()) {
                        throw new IllegalArgumentException("The following layer has no associated mapping. Aborting pinning " + layer);
                    }
                    for (MappedLayer mappedLayer : mappedLayers) {
                        log(Level.FINER, String.format("Pinning layer %s:", mappedLayer));
                        pinLayer(statement, time, mappedLayer);
                        count++;
                        //TODO: configure batchsize
                        if (count == 10) {
                            statement.executeBatch();
                            count = 0;
                      }
                    }
                }
            }
            if (count > 0) {
                statement.executeBatch();
            }
        }
    }

    private void pinLayer(Statement statement, String time, MappedLayer layer) throws SQLException {
        Instant instant = Instant.parse(time);
        // TODO: check config returning numbers
        Instant end = instant.plus(300, ChronoUnit.MINUTES);
        Instant start = instant.minus(300, ChronoUnit.MINUTES);
        String updateQuery = String.format(UPDATE_PINS_SQL, layer.getTableName(), "+", layer.getTemporalAttribute(), start, end);
        log(Level.FINE, String.format("Pinning layer %s in range (%s,%s)", layer.getGeoServerLayerIdentifier(), start, end));
        statement.addBatch(updateQuery);
    }

    private void addView(Connection conn, Long viewId, String time, List<String> layers) throws Exception {
            try (PreparedStatement pstmt = conn.prepareStatement(ADD_VIEW_SQL)){
                Instant instant = Instant.parse(time);
                Instant now = Instant.now();
                Timestamp timestamp = Timestamp.from(instant);
                pstmt.setTimestamp(1, timestamp);
                pstmt.setTimestamp(2, timestamp);
                pstmt.setLong(3, viewId);
                pstmt.setArray(4, conn.createArrayOf("text", layers.toArray()));
                pstmt.setTimestamp(5, Timestamp.from(now));
                pstmt.executeUpdate();
            }
    }

    private void resetPins(Connection conn) throws SQLException {
        Map<String, List<MappedLayer>> layers = layersMapper.getLayers();
        for (List<MappedLayer> mappedLayers: layers.values()) {
            for (MappedLayer mappedLayer: mappedLayers) {
                resetPin(mappedLayer, conn);
            }
        }
    }

    private void resetPin(MappedLayer layer, Connection conn) throws SQLException {
        String tableName = layer.getTableName();
        String resetSql = RESET_PINS_SQL.replace("%s", tableName);
        try (PreparedStatement pstmt = conn.prepareStatement(resetSql)) {
            log(Level.FINE,String.format("Resetting pin for table %s", tableName));
            int result = pstmt.executeUpdate();
            if (result >= 0) {
                log(Level.FINE,String.format("Resetting pin for table %s affected %d rows", tableName, result));
            }
        }

    }

    // Get the status of the current maintenance task
    public String getStatus(UUID uuid) {
        UUID currentId = currentTaskId.get();
        if (!uuid.equals(currentId)) {
            log(Level.WARNING,
                    String.format("Requested id %s doesn't match current id %s ", currentId, uuid));
        }
        return taskStatus.get();
    }

    private boolean resetView(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            log(Level.FINE, "Truncating views table");
            stmt.executeUpdate(TRUNCATE_TABLE_VIEWS);
            return true;
        }
    }

    private boolean acquireLock(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            stmt.setInt(1, PINNING_LOCK_ID);
            log(Level.FINEST, "Acquiring Advisory Lock");
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getBoolean(1)) {
                log(Level.FINEST,"Advisory Lock acquired");
                return true;
            } else {
                log(Level.FINE,"Unable to acquire the Lock. Is the lock already in use?");
                return false;
            }
        }
    }

    private void releaseLock(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            log(Level.FINEST,"Releasing Advisory Lock");
            stmt.setInt(1, PINNING_LOCK_ID);
            stmt.execute();
            log(Level.FINEST,"Advisory Lock released");
        } catch (Exception e) {
            log(Level.SEVERE,"Unable to release the Advisory Lock due to " + e.getLocalizedMessage());
        }
    }

    private void log(Level loggingLevel, String message) {
        if (LOGGER.isLoggable(loggingLevel)) {
            String logMessage = String.format("%s: %s", currentTaskId.get(), message);
            LOGGER.log(loggingLevel, logMessage);
        }
    }

}
