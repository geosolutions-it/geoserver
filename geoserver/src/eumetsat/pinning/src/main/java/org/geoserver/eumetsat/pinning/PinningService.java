package org.geoserver.eumetsat.pinning;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
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
import org.geoserver.eumetsat.pinning.views.View;
import org.geoserver.eumetsat.pinning.views.ViewsClient;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PinningService {

    private static final String TRUNCATE_TABLE_VIEWS = "TRUNCATE table views;";
    private static final String RESET_PINS_SQL = "UPDATE %s SET pin=0 WHERE pin > 0;";
    private static final String UPDATE_PINS_SQL = "UPDATE %s SET pin = pin %s 1 WHERE %s between %s and %s;";
    //TODO: get schema/table from configuration
    private static final String ADD_VIEW_SQL = "INSERT INTO public.\"views\" (time_original, time_main, view_id, layers_list, last_updated) VALUES (?, ?, ?, ?, ?)";

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
                LOGGER.fine("Another process is holding the lock");
                return Optional.empty();
            }
            taskId = UUID.randomUUID();

            currentTaskId.set(taskId);
            taskStatus.set("RUNNING");

            CompletableFuture.runAsync(
                    () -> {
                        try {
                            resetPinning(conn);
                            conn.commit();
                            taskStatus.set("COMPLETED");
                        } catch (Exception e) {
                            taskStatus.set("FAILED");
                            if (conn != null) {
                                try {
                                    conn.rollback();  // Rollback on error
                                } catch (SQLException ex) {
                                    log(Level.SEVERE, "Exception occurred while rolling-back: " + ex.getLocalizedMessage());
                                }
                            }
                            throw new RuntimeException(
                                    "Exception occurred while resetting the views", e);
                        } finally {
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
                    });
        } catch (SQLException e) {
            LOGGER.severe("An exception occurred: " + e);
        }

        return Optional.of(taskId);
    }

    private void resetPinning(Connection conn) throws Exception {
        log(Level.INFO, "Global RESET has been triggered");

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
                log(Level.FINER, "Inserting view" + view);
                ParsedView parsed = viewsClient.parseView(view);
                String viewId = parsed.getViewId();
                String time = parsed.getLockedTime();
                List<String> layers = parsed.getLayers();
                addView(conn, viewId, time, layers);


                for (String layer : layers) {
                    // TODO CHECK NPE HERE
                    List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
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
        Instant start = instant.plus(300, ChronoUnit.MINUTES);
        Instant end = instant.minus(300, ChronoUnit.MINUTES);
        Timestamp startTimestamp = Timestamp.from(start);
        Timestamp endTimestamp = Timestamp.from(end);
        log(Level.FINE, String.format("Pinning layer %s in range (%s,%s)", layer.getGeoServerLayerIdentifier(), startTimestamp, endTimestamp));
        String updateQuery = String.format(UPDATE_PINS_SQL, layer.getTableName(), "+", layer.getTemporalAttribute(), startTimestamp, endTimestamp);
        statement.addBatch(updateQuery);
    }

    private void addView(Connection conn, String viewId, String time, List<String> layers) throws Exception {
            try (PreparedStatement pstmt = conn.prepareStatement(ADD_VIEW_SQL)){
                Instant instant = Instant.parse(time);
                Instant now = Instant.now();
                Timestamp timestamp = Timestamp.from(instant);
                pstmt.setTimestamp(1, timestamp);
                pstmt.setTimestamp(2, timestamp);
                pstmt.setString(3, viewId);
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
            log(Level.FINE, "Acquiring Advisory Lock");
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getBoolean(1)) {
                log(Level.FINE,"Advisory Lock acquired");
                return true;
            } else {
                log(Level.FINE,"Unable to acquire the Lock. Is the lock already in use?");
                return false;
            }
        }
    }

    private void releaseLock(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            log(Level.FINE,"Releasing Advisory Lock");
            stmt.setInt(1, PINNING_LOCK_ID);
            stmt.execute();
            log(Level.FINE,"Advisory Lock released");
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
