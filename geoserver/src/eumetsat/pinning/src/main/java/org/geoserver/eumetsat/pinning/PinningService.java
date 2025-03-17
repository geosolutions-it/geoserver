package org.geoserver.eumetsat.pinning;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
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
import org.geoserver.eumetsat.pinning.views.View;
import org.geoserver.eumetsat.pinning.views.ViewsClient;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PinningService {

    private static final String TRUNCATE_TABLE_VIEWS = "TRUNCATE table views;";
    private static final String RESET_PINS_SQL = "UPDATE %s SET pin=0 WHERE pin > 0;";

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

    @Async
    public Optional<UUID> reset() throws SQLException {
        UUID taskId = null;
        Connection conn = config.dataSource().getConnection();
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
                            taskStatus.set("COMPLETED");
                        } catch (Exception e) {
                            taskStatus.set("FAILED");
                            throw new RuntimeException(
                                    "Exception occurred while resetting the views", e);
                        } finally {
                            currentTaskId.set(null);
                            releaseLock(conn);
                            try {
                                conn.close();
                            } catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
        } catch (SQLException e) {
            LOGGER.severe("An exception occurred: " + e);
        }

        return Optional.of(taskId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
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
    }

    private void resetPins(Connection conn) throws SQLException {
        Map<String, MappedLayer> layers = layersMapper.getLayers();
        for (MappedLayer layer: layers.values()) {
           resetPin(layer, conn);

        }
    }

    private void resetPin(MappedLayer layer, Connection conn) throws SQLException {
        String tableName = layer.getTableName();
        String resetSql = RESET_PINS_SQL.replace("%s", tableName);
        try (PreparedStatement pstmt = conn.prepareStatement(resetSql)) {
            log(Level.FINE,String.format("Resetting pin for table %s", tableName));
            int result = pstmt.executeUpdate();
            if (result >= 0) {
                log(Level.FINE,String.format("Resetting pin for table %s successfully executed", tableName));
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
            log(Level.FINE, "Truncated views table");
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
