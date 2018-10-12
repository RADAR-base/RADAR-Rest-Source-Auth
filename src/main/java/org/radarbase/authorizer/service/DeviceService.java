package org.radarbase.authorizer.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

import org.radarbase.authorizer.domain.DeviceUser;
import org.radarbase.authorizer.repository.DeviceUserRepository;
import org.radarbase.authorizer.service.dto.DeviceAccessToken;
import org.radarbase.authorizer.service.dto.DeviceUserPropertiesDTO;
import org.radarbase.authorizer.service.dto.TokenDTO;
import org.radarbase.authorizer.webapp.exception.NotFoundException;
import org.radarbase.authorizer.webapp.exception.TokenException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DeviceService {
    private final Logger log = LoggerFactory.getLogger(DeviceService.class);

    @Autowired
    private DeviceUserRepository deviceUserRepository;

    @Autowired
    private DeviceClientService authorizationService;

    @Transactional(readOnly = true)
    public List<DeviceUserPropertiesDTO> getAllDevices() {
        log.info("Querying all saved devices");
        return this.deviceUserRepository.findAll().stream().map(DeviceUserPropertiesDTO::new)
                .collect(Collectors.toList());
    }

    public DeviceUserPropertiesDTO save(DeviceUserPropertiesDTO deviceUserPropertiesDTO) {
        DeviceUser deviceUser =
                this.deviceUserRepository.save(new DeviceUser(deviceUserPropertiesDTO));
        return new DeviceUserPropertiesDTO(deviceUser);
    }

    @Transactional
    public DeviceUserPropertiesDTO authorizeAndStoreDevice(@NotNull String code,
            @NotNull String deviceType) {
        DeviceAccessToken accessToken =
                authorizationService.getAccessTokenWithAuthorizeCode(code, deviceType);

        if (accessToken != null) {

            Optional<DeviceUser> existingUser = deviceUserRepository
                    .findByDeviceTypeAndExternalUserId(deviceType, accessToken.getExternalUserId());

            DeviceUser resultUser;
            if (existingUser.isPresent()) {
                resultUser = existingUser.get();
                resultUser.safeUpdateTokenDetails(accessToken);
            } else {
                resultUser = new DeviceUser()
                        .authorized(true)
                        .externalUserId(accessToken.getExternalUserId())
                        .deviceType(deviceType)
                        .startDate(Instant.now());
                resultUser.safeUpdateTokenDetails(accessToken);

                resultUser = this.deviceUserRepository.save(resultUser);
            }
            return new DeviceUserPropertiesDTO(resultUser);
        } else {
            throw new TokenException();
        }
    }

    @Transactional(readOnly = true)
    public DeviceUserPropertiesDTO getDeviceUserById(Long id) {
        Optional<DeviceUser> user = deviceUserRepository.findById(id);

        if (user.isPresent()) {
            return new DeviceUserPropertiesDTO(user.get());
        } else {
            throw new NotFoundException("DeviceUser not found with id " + id);
        }
    }

    @Transactional
    public DeviceUserPropertiesDTO updateDeviceUser(Long id,
            DeviceUserPropertiesDTO deviceUserDto) {

        Optional<DeviceUser> deviceUser = deviceUserRepository.findById(id);

        if (deviceUser.isPresent()) {
            DeviceUser deviceUserToSave = deviceUser.get();
            deviceUserToSave.safeUpdateProperties(deviceUserDto);
            return new DeviceUserPropertiesDTO(deviceUserRepository.save(deviceUserToSave));
        } else {
            throw new NotFoundException(
                    "Unable to update deviceUser. DeviceUser not found with " + "id "
                            + deviceUserDto.getId());
        }

    }

    /**
     * TODO Implement this
     *
     * @param id
     * @return
     */
    public Object revokeTokenAndDeleteUser(Long id) {
        return null;
    }

    @Transactional(readOnly = true)
    public TokenDTO getDeviceTokenByUserId(Long id) {
        Optional<DeviceUser> user = deviceUserRepository.findById(id);

        if (user.isPresent()) {
            DeviceUser deviceUser = user.get();
            return new TokenDTO()
                    .accessToken(deviceUser.getAccessToken())
                    .refreshToken(deviceUser.getRefreshToken())
                    .expiresAt(deviceUser.getExpiresAt());
        } else {
            throw new NotFoundException("DeviceUser not found with id " + id);
        }
    }

    @Transactional
    public TokenDTO refreshTokenForUser(Long id) {
        Optional<DeviceUser> user = deviceUserRepository.findById(id);
        if (user.isPresent()) {
            DeviceUser deviceUser = user.get();
            // refresh token by user id and device-type
            DeviceAccessToken accessToken = authorizationService
                    .refreshToken(deviceUser.getRefreshToken(), deviceUser.getDeviceType());
            // update token
            if (accessToken != null) {
                deviceUser.safeUpdateTokenDetails(accessToken);
                deviceUser = this.deviceUserRepository.save(deviceUser);
                return new TokenDTO()
                        .accessToken(deviceUser.getAccessToken())
                        .refreshToken(deviceUser.getRefreshToken())
                        .expiresAt(deviceUser.getExpiresAt());
            } else {
                throw new TokenException("Could not refresh token successfully");
            }

        } else {
            throw new NotFoundException("DeviceUser not found with id " + id);
        }
    }
}
