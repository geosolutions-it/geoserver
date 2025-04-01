/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning;

import static org.geoserver.eumetsat.pinning.ViewsEvaluator.VIEWS_TABLE;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.TimeZone;
import org.geoserver.eumetsat.pinning.views.ViewRecord;

/**
 * Manages batch operations for database views, handling insert, update, and delete statements for a
 * specific views table with configurable batch processing.
 */
class ViewsBatcher implements Closeable {

    private int batchSize;
    private Calendar utcCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));

    private Connection connection;
    private PreparedStatement insertStatement;
    private int insCount;
    private PreparedStatement deleteStatement;
    private int deleteCount;
    private PreparedStatement updateStatement;
    private int updateCount;

    private static final String ADD_VIEW_QUERY =
            "INSERT INTO "
                    + VIEWS_TABLE
                    + " (time_original, time_main, view_id, layers_list, last_update, driving_layer) VALUES (?, ?, ?, ?, ?, ?)";

    private static final String UPDATE_VIEW_QUERY =
            "UPDATE "
                    + VIEWS_TABLE
                    + " SET time_original = ?, time_main = ?, layers_list = ?, last_update = ? WHERE view_id = ?";

    private static final String DELETE_VIEW_QUERY =
            "DELETE FROM " + VIEWS_TABLE + " WHERE view_id = ?";

    public ViewsBatcher(Connection connection, int batchSize) throws SQLException {
        this.connection = connection;
        this.batchSize = batchSize;
        this.insertStatement = connection.prepareStatement(ADD_VIEW_QUERY);
        this.updateStatement = connection.prepareStatement(UPDATE_VIEW_QUERY);
        this.deleteStatement = connection.prepareStatement(DELETE_VIEW_QUERY);
    }

    /**
     * Releases database statements by closing insert, delete, and update statements.
     *
     * <p>Attempts to close all prepared statements, capturing and aggregating any SQLExceptions
     * that occur during the process. If multiple exceptions are encountered, the first exception is
     * thrown with subsequent exceptions added as suppressed exceptions.
     *
     * @throws SQLException if an error occurs while closing any of the prepared statements
     */
    @Override
    public void close() throws IOException {
        SQLException firstException = null;

        try {
            if (insertStatement != null) {
                insertStatement.close();
            }
        } catch (SQLException e) {
            firstException = e; // Capture the first exception
        }

        try {
            if (deleteStatement != null) {
                deleteStatement.close();
            }
        } catch (SQLException e) {
            if (firstException != null) {
                firstException.addSuppressed(e); // Add to suppressed exceptions
            } else {
                firstException = e;
            }
        }

        try {
            if (updateStatement != null) {
                updateStatement.close();
            }
        } catch (SQLException e) {
            if (firstException != null) {
                firstException.addSuppressed(e); // Add to suppressed exceptions
            } else {
                firstException = e;
            }
        }

        // If any exception occurred, throw the first one (with suppressed exceptions)
        if (firstException != null) {
            throw new IOException(firstException);
        }
    }

    /**
     * Updates a view record in the database using batch processing.
     *
     * <p>Prepares an update statement for a view with its associated metadata, adds it to a batch,
     * and executes the batch when the batch size is reached.
     *
     * @param view The view record to be updated
     * @return A SQL query string representing the update operation
     * @throws SQLException If a database access error occurs
     */
    public String updateView(ViewRecord view) throws SQLException {
        long id = view.getId();
        Timestamp timeOriginal = Timestamp.from(view.getTimeOriginal());
        Timestamp timeMain = Timestamp.from(view.getTimeMain());
        Timestamp lastUpdate = Timestamp.from(view.getLastUpdate());
        Array layersArray = connection.createArrayOf("text", view.getLayers().toArray());
        updateStatement.setTimestamp(1, timeOriginal, utcCalendar);
        updateStatement.setTimestamp(2, timeMain, utcCalendar);
        updateStatement.setArray(3, layersArray);
        updateStatement.setTimestamp(4, lastUpdate, utcCalendar);
        updateStatement.setLong(5, id);
        updateStatement.addBatch();
        if (updateCount++ == batchSize) {
            updateStatement.executeBatch();
            updateCount = 0;
        }
        return buildUpdateViewQuery(timeMain, layersArray, lastUpdate, id);
    }

    /**
     * Inserts a view record into the database using batch processing.
     *
     * <p>Prepares an insert statement for a view with its associated metadata, adds it to a batch,
     * and executes the batch when the batch size is reached.
     *
     * @param view The view record to be inserted
     * @return A SQL query string representing the insert operation
     * @throws SQLException If a database access error occurs
     */
    public String insertView(ViewRecord view) throws SQLException {
        long id = view.getId();
        Timestamp timeMain = Timestamp.from(view.getTimeMain());
        Timestamp timeOriginal = Timestamp.from(view.getTimeOriginal());
        Timestamp lastUpdate = Timestamp.from(view.getLastUpdate());
        Array layersArray = connection.createArrayOf("text", view.getLayers().toArray());
        insertStatement.setTimestamp(1, timeOriginal, utcCalendar);
        insertStatement.setTimestamp(2, timeMain, utcCalendar);
        insertStatement.setLong(3, id);
        insertStatement.setArray(4, connection.createArrayOf("text", view.getLayers().toArray()));
        insertStatement.setTimestamp(5, lastUpdate, utcCalendar);
        insertStatement.setString(6, view.getDrivingLayer());
        insertStatement.addBatch();
        if (insCount++ == batchSize) {
            insertStatement.executeBatch();
            insCount = 0;
        }
        return buildInsertViewQuery(timeOriginal, timeMain, layersArray, lastUpdate, id);
    }

    /**
     * Deletes a view record from the database using batch processing.
     *
     * <p>Prepares a delete statement for a view by its ID, adds it to a batch, and executes the
     * batch when the batch size is reached.
     *
     * @param viewId The ID of the view to be deleted
     * @return A SQL query string representing the delete operation
     * @throws SQLException If a database access error occurs
     */
    public String deleteView(Long viewId) throws SQLException {
        deleteStatement.setLong(1, viewId);
        deleteStatement.addBatch();
        if (deleteCount++ == batchSize) {
            deleteStatement.executeBatch();
            deleteCount = 0;
        }
        return "DELETE FROM " + VIEWS_TABLE + " WHERE id = " + viewId + ";";
    }

    private String buildUpdateViewQuery(
            Timestamp timeMain, Array layersArray, Timestamp lastUpdate, long id) {
        return "UPDATE "
                + VIEWS_TABLE
                + " SET time_main = '"
                + timeMain
                + "', layers = '"
                + layersArray
                + "', last_update = '"
                + lastUpdate
                + "' WHERE id = "
                + id
                + ";";
    }

    private String buildInsertViewQuery(
            Timestamp timeOriginal,
            Timestamp timeMain,
            Array layersArray,
            Timestamp lastUpdate,
            long id) {
        return "INSERT INTO "
                + VIEWS_TABLE
                + " (time_original, time_main, view_id, layers_list, last_update) VALUES ('"
                + timeMain
                + "','"
                + timeOriginal
                + "',"
                + id
                + ",'"
                + layersArray
                + "','"
                + lastUpdate
                + "');";
    }

    public void flush() throws SQLException {
        if (deleteCount > 0) {
            deleteStatement.executeBatch();
            deleteCount = 0;
        }
        if (insCount > 0) {
            insertStatement.executeBatch();
            insCount = 0;
        }
        if (updateCount > 0) {
            updateStatement.executeBatch();
            updateCount = 0;
        }
    }
}
