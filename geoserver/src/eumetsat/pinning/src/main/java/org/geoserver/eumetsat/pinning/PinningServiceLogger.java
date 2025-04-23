/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.eumetsat.pinning.views.ViewRecord;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

@Component
/**
 * Provides logging functionality for the Pinning Service with task-specific log message formatting.
 * This logger supports logging messages at different levels and includes a task identifier in log
 * entries.
 */
public class PinningServiceLogger {

    private static final Logger LOGGER = Logging.getLogger(PinningService.class);

    private UUID taskId;

    public void setTaskId(UUID taskId) {
        this.taskId = taskId;
    }

    public void log(Level loggingLevel, String message) {
        if (LOGGER.isLoggable(loggingLevel)) {
            String logMessage = format(message);
            LOGGER.log(loggingLevel, logMessage);
        }
    }

    public void log(Level loggingLevel, ViewRecord record) {
        if (LOGGER.isLoggable(loggingLevel)) {
            String logMessage = format(record.toString());
            LOGGER.log(loggingLevel, logMessage);
        }
    }

    private String format(String message) {
        return String.format("Pinning[%s]: %s", taskId, message);
    }
}
