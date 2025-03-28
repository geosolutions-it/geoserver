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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.stream.Collectors;
import javax.naming.NamingException;
import org.geoserver.catalog.Catalog;
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.geoserver.eumetsat.pinning.views.ParsedView;
import org.geoserver.eumetsat.pinning.views.TestContext;
import org.geoserver.eumetsat.pinning.views.View;
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

    public static class PinningStatus {
        String uuid;
        String status;

        public PinningStatus(String uuid, String status) {
            this.uuid = uuid;
            this.status = status;
        }

        public String getUuid() {
            return uuid;
        }

        public String getStatus() {
            return status;
        }
    }

    private final AtomicReference<UUID> currentTaskId = new AtomicReference<>(null);
    private final AtomicReference<String> taskStatus = new AtomicReference<>("IDLE");
    private static final int PINNING_LOCK_ID = 9111119;

    @Autowired private PinningServiceLogger logger;

    @Autowired private Catalog catalog;

    @Autowired private PinningServiceConfig config;

    @Autowired private ViewsClient viewsClient;

    @Autowired private ViewsEvaluator viewsEvaluator;

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
            if (!acquireLock(conn)) {
                return Optional.empty();
            }
            taskId = initTask();
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            logger.log(Level.INFO, "Global RESET Started");
                            viewsEvaluator.init(conn);
                            resetPinning(conn);
                            conn.commit();
                            taskStatus.set("COMPLETED");
                            logger.log(Level.INFO, "Global RESET Completed");
                        } catch (Exception e) {
                            processFailure("Global RESET", conn, e);
                        } finally {
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
            if (!acquireLock(conn)) {
                return Optional.empty();
            }
            taskId = initTask();
            CompletableFuture.runAsync(
                    () -> {
                        try {
                            logger.log(Level.INFO, "Incremental pinning Started");
                            viewsEvaluator.init(conn);
                            incrementalPinning(conn);
                            conn.commit();
                            taskStatus.set("COMPLETED");
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
        }

        return Optional.of(taskId);
    }

    private UUID initTask() {
        UUID taskId = UUID.randomUUID();
        logger.setTaskId(taskId);
        currentTaskId.set(taskId);
        taskStatus.set("RUNNING");
        return taskId;
    }

    private void processFailure(String pinningType, Connection conn, Exception e) {
        taskStatus.set("FAILED");
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

    private void incrementalPinning(Connection conn) throws Exception {
        logger.log(Level.FINE, "Retrieve last updated time");
        Timestamp timestamp = null;
        // TODO: This section is only for testing.
        String timestampString = TestContext.getUpdateTime();
        if (timestampString != null) {
            TestContext.clear();
        } else {
            timestamp = viewsEvaluator.retrieveLastUpdate();
            if (timestamp != null) {
                timestampString =
                        timestamp
                                .toInstant()
                                .atOffset(ZoneOffset.UTC)
                                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }
        if (timestampString != null) {
            logger.log(Level.FINE, "Fetching views with lastUpdated > " + timestampString);
        } else {
            logger.log(Level.FINE, "Fetching views ");
        }

        Instant lastUpdate = Instant.now();
        List<View> remoteViews = viewsClient.fetchViews(timestampString);
        logger.log(
                Level.INFO,
                String.format(
                        "Retrieved %d views from the preferences endpoint", remoteViews.size()));

        List<ParsedView> sortedParsedViews = parseAndSort(remoteViews);
        for (ParsedView view : sortedParsedViews) {
            if (isLiveView(view)) {
                continue;
            }
            Long viewId = view.getViewId();
            boolean disabled = view.getDisabled();
            if (disabled) {
                logger.log(
                        Level.INFO,
                        "The following view has been disabled since last update: " + viewId);
                viewsEvaluator.deleteViewAndUnpin(viewId);
            } else {
                ViewRecord viewRecord = viewsEvaluator.fetchView(viewId);
                if (viewRecord == null) {
                    // That's a new view. Add it.
                    logger.log(
                            Level.INFO, "A new view has been added since last update: " + viewId);

                    viewRecord = viewsEvaluator.buildView(view);
                    viewsEvaluator.addViewAndPin(viewRecord);
                } else {
                    // Need to proceed with the diffs.
                    logger.log(
                            Level.INFO,
                            "The following view has been modified since last update: " + viewId);
                    viewsEvaluator.syncView(viewRecord, view);
                }
            }
            viewsEvaluator.flushBatches();
            viewsEvaluator.updateLastUpdate(lastUpdate);
        }
    }

    private List<ParsedView> parseAndSort(List<View> remoteViews) throws IllegalArgumentException {
        return remoteViews.stream()
                .map(viewsClient::parseView) // Convert View -> ParsedView
                .sorted(Comparator.comparing(ParsedView::getLastUpdate)) // Sort by lastUpdate
                .collect(Collectors.toList());
    }

    private void resetPinning(Connection conn) throws Exception {

        logger.log(Level.INFO, "Resetting the views");
        viewsEvaluator.truncateViews();

        Instant lastUpdate = Instant.now();
        List<View> remoteViews = viewsClient.fetchViews(null);
        logger.log(
                Level.INFO,
                String.format(
                        "Retrieved %d views from the preferences endpoint", remoteViews.size()));

        logger.log(Level.INFO, "Resetting the pins");
        viewsEvaluator.fullPinReset();
        logger.log(Level.INFO, "Inserting views");

        for (View remoteView : remoteViews) {
            ParsedView view = viewsClient.parseView(remoteView);
            if (isLiveView(view)) {
                continue;
            }

            ViewRecord viewRecord = viewsEvaluator.buildView(view);
            viewsEvaluator.addViewAndPin(viewRecord);
        }
        viewsEvaluator.flushBatches();
        viewsEvaluator.updateLastUpdate(lastUpdate);
    }

    /**
     * Retrieves the current status of the pinning process.
     *
     * @return A PinningStatus object containing the current task ID and status
     */
    public PinningStatus getStatus() {
        UUID currentId = currentTaskId.get();
        String status = taskStatus.get();
        return new PinningStatus(
                currentId != null ? currentId.toString() : "No taskID available since last restart",
                status);
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

    private boolean isLiveView(ParsedView parsed) {
        boolean isLiveView = !"absolute".equalsIgnoreCase(parsed.getTimeMode());
        if (isLiveView) {
            // Only event views are pinned.
            // Live views (with mode=latest) don't need that
            logger.log(
                    Level.FINE,
                    String.format(
                            "View with id=%s will be skipped since it's a live view",
                            parsed.getViewId()));
        }
        return isLiveView;
    }
}
