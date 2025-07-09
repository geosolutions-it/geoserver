/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import javax.naming.NamingException;
import org.geoserver.catalog.Catalog;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.geoserver.eumetsat.pinning.views.ParsedView;
import org.geoserver.eumetsat.pinning.views.TestContext;
import org.geoserver.eumetsat.pinning.views.ViewRecord;
import org.geoserver.eumetsat.pinning.views.ViewsClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
/**
 * Service for managing pinning operations in the GeoServer EUMETSAT application. Handles tasks
 * related to view pinning, including reset and status tracking.
 */
public class PinningService {

    public static enum StatusValue {
        NOT_RUN_YET,
        RUNNING,
        COMPLETED,
        FAILED,
    }

    public static class PinningStatus {
        String uuid;
        StatusValue status;
        Instant startTime;

        public PinningStatus(String uuid, StatusValue status, Instant startTime) {
            this.uuid = uuid;
            this.status = status;
            this.startTime = startTime;
        }

        public String getUuid() {
            return uuid;
        }

        public Instant getStartTime() {
            return startTime;
        }

        public StatusValue getStatus() {
            return status;
        }
    }

    private final AtomicReference<UUID> currentTaskId = new AtomicReference<>(null);
    private final AtomicReference<Instant> startTime = new AtomicReference<>(null);
    private final AtomicReference<StatusValue> taskStatus =
            new AtomicReference<>(StatusValue.NOT_RUN_YET);
    private static final int PINNING_LOCK_ID = 9111119;

    @Autowired private PinningServiceLogger logger;

    @Autowired private Catalog catalog;

    @Autowired private PinningServiceConfig config;

    @Autowired private LayersMapper layersMapper;

    @Autowired private ViewsClient viewsClient;

    /**
     * Performs a global reset of pinning operations.
     *
     * @return An Optional containing the task UUID if the reset was initiated successfully, or an
     *     empty Optional if the reset could not be started
     * @throws SQLException if a database access error occurs
     * @throws NamingException if a naming service error occurs
     */
    public Optional<UUID> reset() throws SQLException, NamingException {
        UUID taskId = null;
        Connection conn = config.dataSource().getConnection();
        conn.setAutoCommit(false);

        try {
            ViewsEvaluator viewsEvaluator = new ViewsEvaluator(conn, config, logger, layersMapper);
            if (!acquireLock(conn)) {
                return Optional.empty();
            }
            taskId = initTask();
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            logger.log(Level.INFO, "Global RESET Started");
                            checkLoaded();
                            resetPinning(viewsEvaluator);
                            conn.commit();
                            taskStatus.set(StatusValue.COMPLETED);
                            logger.log(Level.INFO, "Global RESET Completed");
                        } catch (Exception e) {
                            processFailure("Global RESET", conn, e);
                        } finally {
                            viewsEvaluator.release();
                            releaseResources(conn);
                        }
                    });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An exception occurred: " + e);
        }

        return Optional.of(taskId);
    }

    /**
     * Performs an incremental pinning operation.
     *
     * @return An Optional containing the task UUID if the incremental pinning was initiated
     *     successfully, or an empty Optional if the operation could not be started
     * @throws SQLException if a database access error occurs
     * @throws NamingException if a naming service error occurs
     */
    public Optional<UUID> incremental() throws SQLException, NamingException {
        UUID taskId = null;
        Connection conn = config.dataSource().getConnection();
        conn.setAutoCommit(false);
        try {
            ViewsEvaluator viewsEvaluator = new ViewsEvaluator(conn, config, logger, layersMapper);
            if (!acquireLock(conn)) {
                return Optional.empty();
            }
            taskId = initTask();
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            logger.log(Level.INFO, "Incremental pinning Started");
                            checkLoaded();
                            incrementalPinning(viewsEvaluator);
                            conn.commit();
                            taskStatus.set(StatusValue.COMPLETED);
                            logger.log(Level.INFO, "Incremental pinning Completed");
                        } catch (Exception e) {
                            processFailure("Incremental Pinning", conn, e);
                        } finally {
                            viewsEvaluator.release();
                            releaseResources(conn);
                        }
                    });
        } catch (SQLException e) {
            logger.log(Level.SEVERE, "An exception occurred: " + e);
            return Optional.empty();
        }

        return Optional.of(taskId);
    }

    private UUID initTask() {
        UUID taskId = UUID.randomUUID();
        logger.setTaskId(taskId);
        currentTaskId.set(taskId);
        startTime.set(Instant.now());
        taskStatus.set(StatusValue.RUNNING);
        return taskId;
    }

    private void processFailure(String pinningType, Connection conn, Exception e) {
        taskStatus.set(StatusValue.FAILED);
        String message = pinningType + " Aborted due to an Exception:" + e.getLocalizedMessage();
        logger.log(Level.SEVERE, message);
        if (conn != null) {
            try {
                conn.rollback();
            } catch (SQLException ex) {
                logger.log(
                        Level.SEVERE,
                        "Exception occurred while rolling-back: " + ex.getLocalizedMessage());
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
    }

    private void incrementalPinning(ViewsEvaluator viewsEvaluator) throws Exception {
        logger.log(Level.FINE, "Retrieve last updated time");
        Instant instant = null;
        String timestampString = TestContext.getUpdateTime();
        if (timestampString != null) {
            // This part allows testing using an imposed lastUpdate filter
            TestContext.clear();
            instant = Instant.parse(timestampString);
        } else {
            Timestamp timestamp = viewsEvaluator.retrieveLastUpdate();
            if (timestamp != null) {
                instant = timestamp.toInstant();
            }
        }
        if (instant != null) {
            logger.log(Level.FINE, "Fetching views with lastUpdated > " + instant);
        } else {
            logger.log(Level.FINE, "Fetching views ");
        }

        Instant lastUpdate = Instant.now();
        List<ParsedView> remoteViews = viewsClient.fetchViews(instant);
        for (ParsedView fetchedView : remoteViews) {
            Long viewId = fetchedView.getViewId();
            boolean disabled = fetchedView.getDisabled();
            if (disabled) {
                logger.log(
                        Level.INFO,
                        "The following view has been disabled since last update: " + viewId);
                viewsEvaluator.deleteViewAndUnpin(viewId);
            } else {
                ViewRecord storedView = viewsEvaluator.fetchView(viewId);
                if (storedView == null) {
                    // That's a new view. Add it.
                    logger.log(
                            Level.INFO, "A new view has been added since last update: " + viewId);

                    storedView = viewsEvaluator.buildView(fetchedView);
                    viewsEvaluator.addViewAndPin(storedView);
                } else if (!storedView
                        .getDrivingLayer()
                        .equalsIgnoreCase(fetchedView.getDrivingLayer())) {
                    // Let's recreate the view
                    viewsEvaluator.deleteViewAndUnpin(storedView.getId());
                    storedView = viewsEvaluator.buildView(fetchedView);
                    viewsEvaluator.addViewAndPin(storedView);
                } else {
                    // Need to proceed with the diffs.
                    logger.log(
                            Level.INFO,
                            "The following view has been modified since last update: " + viewId);
                    viewsEvaluator.syncView(storedView, fetchedView);
                }
            }
            viewsEvaluator.flushBatches();
            viewsEvaluator.updateLastUpdate(lastUpdate);
        }
    }

    private void resetPinning(ViewsEvaluator viewsEvaluator) throws Exception {
        logger.log(Level.INFO, "Resetting the views");
        viewsEvaluator.truncateViews();

        Instant lastUpdate = Instant.now();
        List<ParsedView> remoteViews = viewsClient.fetchViews(null);

        logger.log(Level.INFO, "Resetting the pins");
        viewsEvaluator.fullPinReset();
        logger.log(Level.INFO, "Inserting views");

        for (ParsedView view : remoteViews) {
            if (!view.getDisabled()) {
                ViewRecord viewRecord = viewsEvaluator.buildView(view);
                viewsEvaluator.addViewAndPin(viewRecord);
            }
        }
        viewsEvaluator.flushBatches();
        viewsEvaluator.updateLastUpdate(lastUpdate);
    }

    private void checkLoaded() {
        if (!layersMapper.isLoaded()) {
            throw new RuntimeException(
                    "The layers mapping didn't properly loaded: Aborting pinning. "
                            + "Check the LayersMapper initialization logs.");
        }
    }

    /**
     * Retrieves the current status of the pinning process.
     *
     * @return A PinningStatus object containing the current task ID and status
     */
    public PinningStatus getStatus() {
        UUID currentId = currentTaskId.get();
        StatusValue status = taskStatus.get();
        return new PinningStatus(
                currentId != null ? currentId.toString() : "No taskID available since last restart",
                status,
                startTime.get());
    }

    /**
     * Attempts to acquire an advisory lock for the pinning process.
     *
     * @param conn The database connection to use for acquiring the lock
     * @return true if the lock is successfully acquired, false otherwise
     * @throws SQLException if a database access error occurs
     */
    private boolean acquireLock(Connection conn) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT pg_try_advisory_lock(?)")) {
            stmt.setInt(1, PINNING_LOCK_ID);
            logger.log(Level.FINEST, "Acquiring Advisory Lock");
            ResultSet rs = stmt.executeQuery();
            if (rs.next() && rs.getBoolean(1)) {
                logger.log(Level.FINEST, "Advisory Lock acquired");
                return true;
            } else {
                logger.log(Level.FINE, "Unable to acquire the Lock. Is the lock already in use?");
                UUID taskId = currentTaskId.get();
                logger.log(Level.WARNING, "Another process is holding the lock: " + taskId);
                return false;
            }
        }
    }

    /**
     * Releases the advisory lock on the database connection.
     *
     * @param conn The database connection used to release the lock
     */
    private void releaseLock(Connection conn) {
        try (PreparedStatement stmt = conn.prepareStatement("SELECT pg_advisory_unlock(?)")) {
            logger.log(Level.FINEST, "Releasing Advisory Lock");
            stmt.setInt(1, PINNING_LOCK_ID);
            stmt.execute();
            logger.log(Level.FINEST, "Advisory Lock released");
        } catch (Exception e) {
            logger.log(
                    Level.SEVERE,
                    "Unable to release the Advisory Lock due to " + e.getLocalizedMessage());
        }
    }
}
