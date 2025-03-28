/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.eumetsat.pinning.rest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.eumetsat.pinning.PinningService;
import org.geoserver.eumetsat.pinning.views.TestContext;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.catalog.AbstractCatalogController;
import org.geotools.util.logging.Logging;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/pinning")
public class PinningServiceController extends AbstractCatalogController {

    static final Logger LOGGER = Logging.getLogger(PinningServiceController.class);

    private PinningService pinningService;

    public PinningServiceController(Catalog catalog, PinningService pinningService) {
        super(catalog);
        this.pinningService = pinningService;
    }

    @PostMapping(
            path = "/reset",
            produces = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> reset() throws Exception {
        LOGGER.info("PinningService: Global RESET invoked");
        Optional<UUID> taskId = pinningService.reset();
        if (taskId.isPresent()) {
            return ResponseEntity.ok(taskId.get().toString());
        } else {
            return ResponseEntity.status(409).body("A Reset is already running.");
        }
    }

    @PostMapping(
            path = "/incremental",
            produces = {MediaType.TEXT_PLAIN_VALUE})
    public ResponseEntity<String> incremental(@RequestParam(required = false) String testTime)
            throws Exception {

        if (testTime != null) {
            TestContext.setUpdateTime(testTime);
        }
        LOGGER.info("PinningService: INCREMENTAL pinning invoked");
        Optional<UUID> taskId = pinningService.incremental();
        if (taskId.isPresent()) {
            return ResponseEntity.ok(taskId.get().toString());
        } else {
            return ResponseEntity.status(409).body("An incremental pin is already running.");
        }
    }

    // Check the status of the running maintenance job
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        PinningService.PinningStatus pinningStatus = pinningService.getStatus();
        Map<String, Object> response = new HashMap<>();
        response.put("Status", pinningStatus.getStatus());

        String uuid = pinningStatus.getUuid();
        if (uuid != null) {
            response.put("Identifier", uuid);
        }
        Instant startTime = pinningStatus.getStartTime();
        if (startTime != null) {
            response.put("Start Time", startTime);
        }

        switch (pinningStatus.getStatus()) {
            case FAILED:
                response.put("Result", "Failure. Please check the logs for further details");
                break;
            case COMPLETED:
                response.put("Result", "Success");
                break;
            case NOT_RUN_YET:
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
