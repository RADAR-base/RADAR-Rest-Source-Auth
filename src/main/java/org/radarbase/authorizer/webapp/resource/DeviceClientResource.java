/*
 *
 *  * Copyright 2018 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer.webapp.resource;

import java.util.List;

import org.radarbase.authorizer.service.DeviceClientService;
import org.radarbase.authorizer.service.dto.DeviceClientDetailsDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceClientResource {

    private Logger logger = LoggerFactory.getLogger(DeviceClientResource.class);

    @Autowired
    private DeviceClientService deviceClientService;

    @GetMapping("/device-clients")
    public ResponseEntity<List<DeviceClientDetailsDTO>> getAllDeviceProperties() {
        logger.debug("Get all devices client details");
        return ResponseEntity.ok(this.deviceClientService.getAllDeviceClientDetails());
    }


    @GetMapping("/device-clients/device-type")
    public ResponseEntity<List<String>> getAllAvailableDeviceTypes() {
        logger.debug("Get all devices-types");
        return ResponseEntity.ok(this.deviceClientService.getAvailableDeviceTypes());
    }

    @GetMapping("/device-clients/{deviceType}")
    public ResponseEntity<DeviceClientDetailsDTO> getDeviceAuthDetailsByDeviceType(
            @PathVariable String deviceType) {
        logger.info("Get device detail by device-type {}", deviceType);
        return ResponseEntity.ok(this.deviceClientService.getAllDeviceClientDetails(deviceType));
    }

}
