package org.radarbase.authorizer.domain;

import java.time.Duration;
import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;
import org.radarbase.authorizer.service.dto.DeviceAccessToken;
import org.radarbase.authorizer.service.dto.DeviceUserPropertiesDTO;

@Data
@Entity
@Table(name = "radar_device_user")
public class DeviceUser {

    @Transient
    private static final Duration EXPIRY_TIME_MARGIN = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // Project ID to be used in org.radarcns.kafka.ObservationKey record keys
    private String projectId;

    // User ID to be used in org.radarcns.kafka.ObservationKey record keys
    private String userId;

    // Source ID to be used in org.radarcns.kafka.ObservationKey record keys
    private String sourceId;

    // Date from when to collect data.
    private Instant startDate;

    // Date until when to collect data.
    private Instant endDate;


    private String deviceType;

    // is authorized by user
    private Boolean authorized = false;

    private String externalUserId;

    private String accessToken;

    private String refreshToken;

    private Integer expiresIn;

    private Instant expiresAt;

    private String tokenType;

    public DeviceUser() {}

    public DeviceUser(DeviceUserPropertiesDTO deviceUserPropertiesDTO) {
        if(deviceUserPropertiesDTO.getId() != null) {
            this.id = deviceUserPropertiesDTO.getId();
        }
        this.projectId = deviceUserPropertiesDTO.getProjectId();
        this.userId = deviceUserPropertiesDTO.getUserId();
        this.sourceId = deviceUserPropertiesDTO.getSourceId();
        this.deviceType = deviceUserPropertiesDTO.getDeviceType();
        this.startDate = deviceUserPropertiesDTO.getStartDate();
        this.endDate = deviceUserPropertiesDTO.getEndDate();
        this.externalUserId = deviceUserPropertiesDTO.getExternalUserId();
        this.authorized = deviceUserPropertiesDTO.isAuthorized();
    }

    public long getId() {
        return id;
    }

    public DeviceUser id(Long id) {
        this.id = id;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public DeviceUser projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public DeviceUser userId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public DeviceUser sourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public DeviceUser startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public DeviceUser endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public DeviceUser deviceType(String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public DeviceUser authorized(Boolean authorized) {
        this.authorized = authorized;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public DeviceUser externalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public DeviceUser accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public DeviceUser refreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public DeviceUser expiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public DeviceUser expiresIn(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public DeviceUser tokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    /**
     * Updates only a subset of user properties during update.
     * Does not update properties such as id and token data.
     * @param deviceUserDto user details to update.
     */
    public void safeUpdateProperties(DeviceUserPropertiesDTO deviceUserDto) {
        this.projectId = deviceUserDto.getProjectId();
        this.userId = deviceUserDto.getUserId();
        this.sourceId = deviceUserDto.getSourceId();
        this.startDate = deviceUserDto.getStartDate();
        this.endDate = deviceUserDto.getEndDate();
    }


    /**
     * Updates only the token related properties during update.
     * Does not update properties such as id and study information.
     * @param deviceAccessToken token details to update.
     */
    public void safeUpdateTokenDetails(DeviceAccessToken deviceAccessToken) {
        this.accessToken = deviceAccessToken.getAccessToken();
        this.refreshToken = deviceAccessToken.getRefreshToken();
        this.expiresIn = deviceAccessToken.getExpiresIn();
        this.expiresAt = Instant.now()
                .plusSeconds(deviceAccessToken.getExpiresIn())
                .minus(EXPIRY_TIME_MARGIN);
    }
}
