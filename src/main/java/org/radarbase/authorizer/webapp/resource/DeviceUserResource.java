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

import java.net.URI;
import java.net.URISyntaxException;
import javax.validation.Valid;

import org.radarbase.authorizer.service.DeviceUserService;
import org.radarbase.authorizer.service.dto.DeviceUserPropertiesDTO;
import org.radarbase.authorizer.service.dto.DeviceUsersDTO;
import org.radarbase.authorizer.service.dto.TokenDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DeviceUserResource {

    private Logger logger = LoggerFactory.getLogger(DeviceUserResource.class);

    @Autowired
    private DeviceUserService deviceUserService;

    @PostMapping("/users")
    public ResponseEntity addAuthorizedDeviceUser(@RequestParam(value = "code") String code,
            @RequestParam(value = "state") String state) throws URISyntaxException {
        logger.debug("Add a device user with code {} and state {}", code, state);
        DeviceUserPropertiesDTO user = this.deviceUserService.authorizeAndStoreDevice(code, state);
        return ResponseEntity
                .created(new URI("/user/" + user.getId())).body(user);
    }

    @GetMapping("/users")
    public ResponseEntity<DeviceUsersDTO> getAllDeviceProperties(
            @RequestParam(value = "source-type", required = false) String deviceType) {
        if (deviceType != null && !deviceType.isEmpty()) {
            logger.debug("Get all users by type {}", deviceType);
            return ResponseEntity
                    .ok(this.deviceUserService.getAllUsersByDeviceType(deviceType));
        }

        logger.debug("Get all device users");
        return ResponseEntity
                .ok(this.deviceUserService.getAllDevices());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<DeviceUserPropertiesDTO> getDevicePropertiesById(
            @PathVariable String id) {
        logger.debug("Get device user with id {}", id);
        return ResponseEntity
                .ok(this.deviceUserService.getDeviceUserById(Long.valueOf(id)));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity updateDeviceUser(@Valid @PathVariable String id,
            @RequestBody DeviceUserPropertiesDTO deviceUser) {
        logger.debug("Requesting to update deviceUser");
        return ResponseEntity
                .ok(this.deviceUserService.updateDeviceUser(Long.valueOf(id), deviceUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteDeviceUser(@Valid @PathVariable String id) {
        logger.debug("Requesting to delete deviceUser");
        this.deviceUserService.revokeTokenAndDeleteUser(Long.valueOf(id));
        return ResponseEntity
                .ok().header("user-removed", id).build();
    }


    @GetMapping("/users/{id}/token")
    public ResponseEntity<TokenDTO> getUserToken(@PathVariable String id) {
        logger.debug("Get user token for id {}", id);
        return ResponseEntity
                .ok(this.deviceUserService.getDeviceTokenByUserId(Long.valueOf(id)));
    }

    @PostMapping("/users/{id}/token")
    public ResponseEntity<TokenDTO> requestRefreshTokenForUser(@PathVariable String id) {
        logger.debug("Refreshing user token for id {}", id);
        return ResponseEntity
                .ok(this.deviceUserService.refreshTokenForUser(Long.valueOf(id)));
    }
}