package org.radarbase.authorizer.webapp.resource;

import java.util.List;
import java.util.stream.Collectors;

import org.radarbase.authorizer.service.DeviceService;
import org.radarbase.authorizer.service.dto.DevicePropertiesDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceResource {

    private Logger logger = LoggerFactory.getLogger(DeviceResource.class);

    @Autowired
    private DeviceService deviceService;

    @GetMapping("/devices")
    public ResponseEntity<List<DevicePropertiesDTO>> getAllDeviceProperties() {
        logger.info("Get all devices");
        return ResponseEntity
                .ok(this.deviceService.getAllDevices());
    }


    @PostMapping("/devices")
    public ResponseEntity<DevicePropertiesDTO> addDevice(DevicePropertiesDTO devicePropertiesDTO) {
        logger.info("Saving new device properties ");
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(this.deviceService.save(devicePropertiesDTO));
    }
}