package org.radarbase.authorizer.webapp.resource;

import org.radarbase.authorizer.service.RestSourceClientService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthCheckResource {
    private Logger logger = LoggerFactory.getLogger(HealthCheckResource.class);

    @Autowired
    private RestSourceClientService restSourceClientService;

    @GetMapping("/health")
    public ResponseEntity getAllDeviceProperties() {
        logger.debug("Health check");
        this.restSourceClientService.getAllRestSourceClientDetails();
        return ResponseEntity.ok("Alive");
    }

}
