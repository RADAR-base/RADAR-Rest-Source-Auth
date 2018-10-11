package org.radarbase.authorizer.webapp.resource;

import java.util.List;

import org.radarbase.authorizer.service.DeviceAuthorizationService;
import org.radarbase.authorizer.service.dto.DeviceClientDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceAuthorizationResource {

    private Logger logger = LoggerFactory.getLogger(DeviceAuthorizationResource.class);

    @Autowired
    private DeviceAuthorizationService deviceAuthorizationService;

    @GetMapping("/device-clients")
    public ResponseEntity<List<DeviceClientDetailsDTO>> getAllDeviceProperties() {
        logger.info("Get all devices client details");
        return ResponseEntity
                .ok(this.deviceAuthorizationService.getAllDeviceClientDetails());
    }


    @GetMapping("/device-clients/device-type")
    public ResponseEntity<List<String>> getAllAvailableDeviceTypes() {
        logger.info("Get all devices client details");
        return ResponseEntity
                .ok(this.deviceAuthorizationService.getAvailableDeviceTypes());
    }

    @GetMapping("/device-clients/{deviceType}")
    public ResponseEntity<DeviceClientDetailsDTO> getDeviceAuthDetailsByDeviceType(@PathVariable String
            deviceType) {
        logger.info("Get device detail by device-type {}", deviceType);
        return ResponseEntity
                .ok(this.deviceAuthorizationService.getAllDeviceClientDetails(deviceType));
    }

    @GetMapping("/callback")
    public ResponseEntity processCallback(
            @RequestParam(value = "code", required = true) String code,
            @RequestParam(value = "state", required = false) String state) {
        logger.info("request to get access token with code {} and state {}" , code, state);
        return ResponseEntity
                .ok(this.deviceAuthorizationService.authorizeAndStoreDevice(code, state));
    }


}
