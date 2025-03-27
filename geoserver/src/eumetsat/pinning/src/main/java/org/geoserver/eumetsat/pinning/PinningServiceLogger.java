package org.geoserver.eumetsat.pinning;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.eumetsat.pinning.views.ViewRecord;
import org.geotools.util.logging.Logging;
import org.springframework.stereotype.Component;

@Component
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
        return String.format("Pinning run: %s: %s", taskId, message);
    }
}
