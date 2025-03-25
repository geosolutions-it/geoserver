package org.geoserver.eumetsat.pinning;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
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
            String logMessage = String.format("%s: %s", taskId, message);
            LOGGER.log(loggingLevel, logMessage);
        }
    }

}
