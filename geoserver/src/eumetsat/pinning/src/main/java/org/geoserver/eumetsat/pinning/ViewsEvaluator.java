/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
import org.geoserver.eumetsat.pinning.config.PinningServiceConfig;
import org.geoserver.eumetsat.pinning.views.ParsedView;
import org.geoserver.eumetsat.pinning.views.ViewRecord;

/**
 * Evaluates views and operate on the views (insert, update, delete), performing the necessary
 * pinning operations on the involved records
 */
public class ViewsEvaluator {

    /**
     * Tracks and provides details about layer changes between two sets of layers. Identifies added,
     * removed, and unchanged layers during an update process.
     */
    public class LayersUpdate {

        private Set<String> removed;
        private Set<String> added;
        private Set<String> unchanged;

        /**
         * Constructs a LayersUpdate instance to track layer changes between two sets of layers.
         *
         * @param layersListBefore The list of layer names before the update
         * @param layersListAfter The list of layer names after the update
         */
        public LayersUpdate(List<String> layersListBefore, List<String> layersListAfter) {
            HashSet<String> layersBefore = new HashSet<>(layersListBefore);
            HashSet<String> layersAfter = new HashSet<>(layersListAfter);

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

    private LayersMapper layersMapper;

    private PinningServiceLogger logger;

    private Connection connection;

    private Duration duration;

    private PinBatcher pinBatcher;

    private ViewsBatcher viewsBatcher;

    public ViewsEvaluator(
            Connection connection,
            PinningServiceConfig config,
            PinningServiceLogger logger,
            LayersMapper mapper)
            throws SQLException {
        this.logger = logger;
        this.layersMapper = mapper;
        this.connection = connection;
        this.duration = Duration.ofSeconds(config.pinningMinutes() * 30);
        this.pinBatcher = new PinBatcher(connection, config.batchSize());
        this.viewsBatcher = new ViewsBatcher(connection, config.batchSize());
    }

    /**
     * Release the resources by closing underlying statements/preparedStatements in batcher classes
     *
     * @throws RuntimeException
     */
    public void release() throws RuntimeException {
        IOException firstException = null;
        try {
            this.pinBatcher.close();
        } catch (IOException e) {
            firstException = e;
        }

        try {
            this.viewsBatcher.close();
        } catch (IOException e) {
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

    /**
     * Make sure to complete pending batches
     *
     * @throws SQLException
     */
    public void flushBatches() throws SQLException {
        viewsBatcher.flush();
        pinBatcher.flush();
    }

    /**
     * Globally reset all Pins in all layers.
     *
     * @throws SQLException
     */
    public void fullPinReset() throws SQLException {
        Map<String, List<MappedLayer>> layers = layersMapper.getLayers();
        for (List<MappedLayer> mappedLayers : layers.values()) {
            for (MappedLayer mappedLayer : mappedLayers) {
                resetPin(mappedLayer);
            }
        }
    }

    /**
     * Add a view and pin the involved layers.
     *
     * @param view
     * @throws Exception
     */
    public void addViewAndPin(ViewRecord view) throws Exception {
        logger.log(Level.FINE, "Pinning layers for view: " + view.getId());
        logger.log(Level.FINER, view);
        updateLayersPins(view.getLayers(), view.getTimeOriginal(), view.getTimeOriginal(), true);
        insertView(view);
    }

    /**
     * Delete a disabled view and unpin the involved layers.
     *
     * @param viewId
     * @throws Exception
     */
    public void deleteViewAndUnpin(Long viewId) throws Exception {
        ViewRecord view = fetchView(viewId);
        updateLayersPins(view.getLayers(), view.getTimeOriginal(), view.getTimeMain(), false);
        deleteView(viewId);
    }

    /**
     * Truncate the views table (as part of a Global Reset)
     *
     * @throws SQLException
     */
    public boolean truncateViews() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            logger.log(Level.INFO, "Truncating views table");
            stmt.executeUpdate(TRUNCATE_TABLE_VIEWS);
            return true;
        }
    }

    public ViewRecord buildView(ParsedView view) {
        // For a newly created view, originalTime and mainTime are the same
        ViewRecord viewRecord =
                new ViewRecord(
                        view.getViewId(),
                        view.getTime(),
                        view.getTime(),
                        view.getLayers(),
                        view.getDrivingLayer(),
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
            updateLayersPin(layer, originalTime, mainTime, addPin);
        }
    }

    /**
     * Sync an existing view with the last updated version of it.
     *
     * @param view
     * @param parsed
     * @throws SQLException
     * @throws IOException
     */
    public void syncView(ViewRecord view, ParsedView parsed) throws SQLException, IOException {
        Instant originalTime = view.getTimeOriginal();
        Instant viewTime = parsed.getTime();
        ViewRecord record = buildView(parsed);
        if (viewTime.equals(view.getTimeMain())) {
            logger.log(Level.FINE, "View time has not been modified");
            // Redo the view since the layers may have been changed.
            // Note that a redo may also result into a no-op
            redoView(view, parsed);
        } else if (!isExtendingWindow(view, originalTime, viewTime)) {
            logger.log(Level.FINE, "View time has been updated outside the pinning window");
            redoView(view, parsed);
        } else {
            logger.log(Level.FINE, "View time has been updated within the pinning window");
            extendView(view, parsed);
            record.setTimeOriginal(originalTime);
        }
        updateView(record);
    }

    /**
     * Fetch a View from DB, given the identifier
     *
     * @param viewId
     * @return
     * @throws Exception
     */
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

                    String drivingLayer = rs.getString("driving_layer");
                    Instant lastUpdated = rs.getTimestamp("last_update", utcCalendar).toInstant();

                    ViewRecord record =
                            new ViewRecord(
                                    id,
                                    timeOriginal,
                                    timeMain,
                                    layersList,
                                    drivingLayer,
                                    lastUpdated);
                    logger.log(Level.FINER, record);
                    return record;
                }
            }
        }
        return null;
    }

    private void setPinLayer(Instant originalTime, Instant mainTime, MappedLayer layer, boolean add)
            throws SQLException, IOException {
        Instant nearestOrig = layer.getNearest(originalTime);
        Instant nearestMain = layer.getNearest(mainTime);
        boolean mainGreaterThanOriginal = nearestMain.isAfter(nearestOrig);
        Instant minTime = mainGreaterThanOriginal ? nearestOrig : nearestMain;
        Instant maxTime = mainGreaterThanOriginal ? nearestMain : nearestOrig;
        Instant start = getLeft(minTime);
        Instant end = getRight(maxTime);
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
                        layer.getFullTableName(), layer.getTemporalAttribute(), start, end, add);
        logger.log(Level.FINEST, "pinning query:" + updateQuery);
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
        String tableName = layer.getFullTableName();
        logger.log(Level.FINE, String.format("Resetting pin for table %s", tableName));
        String resetQuery = pinBatcher.resetPins(tableName);
        logger.log(Level.FINEST, "Resetting pin query: " + resetQuery);
    }

    private void updateLayersPin(
            String layer, Instant originalTime, Instant mainTime, boolean addPin)
            throws SQLException, IOException {
        List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
        for (MappedLayer mappedLayer : mappedLayers) {
            logger.log(Level.FINER, String.format("Updating pins for layer %s:", mappedLayer));
            setPinLayer(originalTime, mainTime, mappedLayer, addPin);
        }
    }

    /**
     * Redo the view by unpinning the layers that are no longer in the view and pinning the layers
     * that get added
     */
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
        updateLayersPins(layersUpdate.getAdded(), parsed.getTime(), parsed.getTime(), true);

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

    /** Extend the pinning window of a View */
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
        updateLayersPins(layersUpdate.getAdded(), view.getTimeOriginal(), parsed.getTime(), true);

        for (String layer : layersUpdate.getUnchanged()) {
            List<MappedLayer> mappedLayers = layersMapper.getLayersById(layer);
            for (MappedLayer mappedLayer : mappedLayers) {
                Instant origTime = mappedLayer.getNearest(view.getTimeOriginal());
                Instant previousMainTime = mappedLayer.getNearest(view.getTimeMain());
                Instant origLeft = getLeft(origTime);
                Instant origRight = getRight(origTime);
                Instant previousMainLeft = getLeft(previousMainTime);
                Instant previousMainRight = getRight(previousMainTime);
                Instant parsedTime = mappedLayer.getNearest(parsed.getTime());
                Instant newLeft = getLeft(parsedTime);
                Instant newRight = getRight(parsedTime);
                boolean timeUnchanged =
                        (previousMainLeft.equals(origLeft) && previousMainRight.equals(origRight));
                if (newLeft.equals(previousMainLeft) && newRight.equals(previousMainRight)) {
                    logger.log(
                            Level.FINE,
                            "The extended view covers same records as before. No action needed on layer: "
                                    + mappedLayer.getLayerName());
                    continue;
                }
                // Here we are checking the "positioning" of the original, main and incoming window
                // so that we can pin [++] and unpin [--] records accordingly.
                // https://docs.google.com/drawings/d/15iXiTLEmja1vHudcBUv7VIangH--rqZ6L_2eTYDVCjw/edit?usp=sharing
                if (newRight.isAfter(origRight)) {
                    if (newRight.isAfter(previousMainRight)) {
                        if (previousMainLeft.isAfter(origLeft) || timeUnchanged) {
                            // [original]
                            //       [ main ]
                            //           [  new  ]
                            //              [ ++ ]
                            setPinLayerRange(
                                    previousMainRight.plus(Duration.ofSeconds(1)),
                                    newRight,
                                    mappedLayer,
                                    true);
                        } else {
                            //     [original]
                            //  [ main ]
                            //           [  new  ]
                            //  [--]        [ ++ ]
                            setPinLayerRange(
                                    previousMainLeft,
                                    origLeft.minus(Duration.ofSeconds(1)),
                                    mappedLayer,
                                    false);
                            setPinLayerRange(
                                    origRight.plus(Duration.ofSeconds(1)),
                                    newRight,
                                    mappedLayer,
                                    true);
                        }
                    } else {
                        //     [original]
                        //             [ main  ]
                        //        [  new  ]
                        //                [ -- ]
                        setPinLayerRange(
                                newLeft.plus(Duration.ofSeconds(1)),
                                previousMainRight,
                                mappedLayer,
                                false);
                    }

                } else {
                    if (newLeft.isBefore(previousMainLeft)) {
                        if (previousMainRight.isBefore(origRight) || timeUnchanged) {
                            //       [original]
                            //    [  main  ]
                            // [  new  ]
                            // [++]
                            setPinLayerRange(
                                    newLeft,
                                    origLeft.minus(Duration.ofSeconds(1)),
                                    mappedLayer,
                                    true);
                        } else {
                            //      [original]
                            //             [  main  ]
                            // [  new  ]
                            // [ ++ ]        [  --  ]
                            setPinLayerRange(
                                    origRight.plus(Duration.ofSeconds(1)),
                                    previousMainRight,
                                    mappedLayer,
                                    false);
                            setPinLayerRange(
                                    newLeft,
                                    origLeft.minus(Duration.ofSeconds(1)),
                                    mappedLayer,
                                    true);
                        }
                    } else {
                        //           [original]
                        //    [  main  ]
                        //         [  new  ]
                        //    [ -- ]
                        setPinLayerRange(
                                previousMainLeft,
                                newLeft.minus(Duration.ofSeconds(1)),
                                mappedLayer,
                                false);
                    }
                }
            }
        }
    }

    private boolean isExtendingWindow(ViewRecord view, Instant originalTime, Instant viewTime)
            throws IOException {
        MappedLayer layer = layersMapper.getLayersById(view.getDrivingLayer()).get(0);
        Instant nearestOrig = layer.getNearest(originalTime);
        Instant nearestMain = layer.getNearest(viewTime);
        Duration distance = Duration.between(nearestOrig, nearestMain).abs();
        return distance.minus(duration.multipliedBy(2)).isNegative();
    }
}
