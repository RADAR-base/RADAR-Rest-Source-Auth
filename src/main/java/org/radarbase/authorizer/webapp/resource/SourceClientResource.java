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

import org.radarbase.authorizer.service.RestSourceClientService;
import org.radarbase.authorizer.service.dto.RestSourceClientDetailsDTO;
import org.radarbase.authorizer.service.dto.RestSourceClients;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SourceClientResource {

    private Logger logger = LoggerFactory.getLogger(SourceClientResource.class);

    @Autowired
    private RestSourceClientService restSourceClientService;

    @GetMapping("/source-clients")
    public ResponseEntity<RestSourceClients> getAllDeviceProperties() {
        logger.debug("Get all source clients details");
        return ResponseEntity.ok(this.restSourceClientService.getAllRestSourceClientDetails());
    }


    @GetMapping("/source-clients/type")
    public ResponseEntity<List<String>> getAllAvailableDeviceTypes() {
        logger.debug("Get all source-types");
        return ResponseEntity.ok(this.restSourceClientService.getAvailableDeviceTypes());
    }

    @GetMapping("/source-clients/{sourceType}")
    public ResponseEntity<RestSourceClientDetailsDTO> getDeviceAuthDetailsByDeviceType(
            @PathVariable String sourceType) {
        logger.info("Get source clients detail by type {}", sourceType);
        return ResponseEntity.ok(this.restSourceClientService.getAllRestSourceClientDetails(sourceType));
    }

}
