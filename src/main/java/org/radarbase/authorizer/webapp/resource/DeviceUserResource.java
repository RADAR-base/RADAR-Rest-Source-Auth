package org.radarbase.authorizer.webapp.resource;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.validation.Valid;

import org.radarbase.authorizer.service.DeviceService;
import org.radarbase.authorizer.service.dto.DeviceAccessToken;
import org.radarbase.authorizer.service.dto.DeviceUserPropertiesDTO;
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
    private DeviceService deviceService;

    @PostMapping("/users")
    public ResponseEntity addAuthorizedDeviceUser(
            @RequestParam(value = "code") String code,
            @RequestParam(value = "state") String state) throws URISyntaxException {
        logger.debug("Add a device user with code {} and state {}" , code, state);
        DeviceUserPropertiesDTO user = this.deviceService.authorizeAndStoreDevice(code, state);
        return ResponseEntity
                .created(new URI("/user/" + user.getId()))
                .body(user);
    }

    @GetMapping("/users")
    public ResponseEntity<List<DeviceUserPropertiesDTO>> getAllDeviceProperties(
            @RequestParam(value="device-type", required = false) String deviceType) {
        if (deviceType != null && !deviceType.isEmpty()) {
            logger.debug("Get all users by device-type {}", deviceType);
            return ResponseEntity
                    .ok(this.deviceService.getAllUsersByDeviceType(deviceType));
        }

        logger.debug("Get all device users");
        return ResponseEntity
                .ok(this.deviceService.getAllDevices());
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<DeviceUserPropertiesDTO> getAllDeviceProperties(
            @PathVariable Long id) {
        logger.debug("Get device user with id {}", id);
        return ResponseEntity
                .ok(this.deviceService.getDeviceUserById(id));
    }

    @PutMapping("/users/{id}")
    public ResponseEntity updateDeviceUser(@Valid @PathVariable Long id,
            @RequestBody DeviceUserPropertiesDTO deviceUser) {
        logger.debug("Requesting to update deviceUser");
        return ResponseEntity
                .ok(this.deviceService.updateDeviceUser(id, deviceUser));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<Void> deleteDeviceUser(@Valid @PathVariable Long id) {
        logger.debug("Requesting to delete deviceUser");
        this.deviceService.revokeTokenAndDeleteUser(id);
        return ResponseEntity
                .ok()
                .header("user-removed" , id.toString()).build();
    }


    @GetMapping("/users/{id}/token")
    public ResponseEntity<TokenDTO> getUserToken(
            @PathVariable Long id) {
        logger.debug("Get user token for id {}", id);
        return ResponseEntity
                .ok(this.deviceService.getDeviceTokenByUserId(id));
    }

    @PostMapping("/users/{id}/token")
    public ResponseEntity<TokenDTO> requestRefreshTokenForUser(
            @PathVariable Long id) {
        logger.debug("Refreshing user token for id {}", id);
        return ResponseEntity
                .ok(this.deviceService.refreshTokenForUser(id));
    }
}