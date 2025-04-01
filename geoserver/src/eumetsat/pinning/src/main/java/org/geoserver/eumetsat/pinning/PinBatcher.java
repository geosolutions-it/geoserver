/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning;

import java.io.Closeable;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;

/**
 * Manages batch operations for updating pin values in a database table.
 *
 * <p>This class provides methods to incrementally update or reset pin values for temporal data
 * within a specified range, using JDBC batch processing.
 */
class PinBatcher implements Closeable {
    private Statement statement;
    private int count;
    private int batchSize;

    private static final String RESET_PINS_QUERY = "UPDATE %s SET pin=0 WHERE pin != 0;";

    private static final String UPDATE_PINS_QUERY =
            "UPDATE %s SET pin = pin %s 1 WHERE %s >= '%s' and %s <= '%s';";

    public PinBatcher(Connection connection, int batchSize) throws SQLException {
        this.batchSize = batchSize;
        this.statement = connection.createStatement();
    }

    public String update(
            String fullTableName, String temporalAttribute, Instant start, Instant end, boolean add)
            throws SQLException {
        String updateQuery =
                String.format(
                        UPDATE_PINS_QUERY,
                        fullTableName,
                        add ? "+" : "-",
                        temporalAttribute,
                        start,
                        temporalAttribute,
                        end);
        addToBatch(updateQuery);
        return updateQuery;
    }

    public String resetPins(String fullTableName) throws SQLException {
        String resetSql = RESET_PINS_QUERY.replace("%s", fullTableName);
        addToBatch(resetSql);
        return resetSql;
    }

    private void addToBatch(String updateQuery) throws SQLException {
        statement.addBatch(updateQuery);
        if (count++ == batchSize) {
            statement.executeBatch();
            count = 0;
        }
    }

    public void flush() throws SQLException {
        if (count > 0) {
            statement.executeBatch();
            count = 0;
        }
    }

    @Override
    public void close() throws IOException {
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                throw new IOException(e);
            }
        }
    }
}
