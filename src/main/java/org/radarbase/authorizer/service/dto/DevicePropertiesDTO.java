package org.radarbase.authorizer.service.dto;


import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.radarbase.authorizer.config.DeviceAuthorizationConfig.DeviceType;
import org.radarbase.authorizer.domain.Device;


public class DevicePropertiesDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    // Unique user key
    private Long id;

    // Project ID to be used in org.radarcns.kafka.ObservationKey record keys
    private String projectId;

    // User ID to be used in org.radarcns.kafka.ObservationKey record keys
    private String userId;

    // Source ID to be used in org.radarcns.kafka.ObservationKey record keys
    private String sourceId;

    // Date from when to collect data.
    private Instant stateDate;

    // Date until when to collect data.
    private Instant endDate;


    private DeviceType deviceType;

    // is authorized by user
    private Boolean authorized = false;

    private String externalUserId;

    public DevicePropertiesDTO () {}

    public DevicePropertiesDTO (Device device) {
        this.id = device.getId();
        this.projectId = device.getProjectId();
        this.userId = device.getUserId();
        this.sourceId = device.getSourceId();
        this.authorized = device.getAuthorized();
        this.deviceType = device.getDeviceType();
        this.endDate = device.getEndDate();
        this.stateDate = device.getStartDate();
        this.externalUserId = device.getExternalUserId();
    }

    public Long getId() {
        return id;
    }

    public DevicePropertiesDTO id(Long id) {
        this.id = id;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public DevicePropertiesDTO projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public DevicePropertiesDTO userId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public DevicePropertiesDTO sourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Instant getStartDate() {
        return stateDate;
    }

    public DevicePropertiesDTO stateDate(Instant stateDate) {
        this.stateDate = stateDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public DevicePropertiesDTO endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }


    public DeviceType getDeviceType() {
        return deviceType;
    }

    public DevicePropertiesDTO deviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public Boolean isAuthorized() {
        return authorized;
    }

    public DevicePropertiesDTO authorized(Boolean isAuthorized) {
        this.authorized = isAuthorized;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public DevicePropertiesDTO externalDeviceId(String externalDeviceId) {
        this.externalUserId = externalDeviceId;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DevicePropertiesDTO that = (DevicePropertiesDTO) o;
        return Objects.equals(id, that.id)
                && Objects.equals(projectId, that.projectId)
                && Objects.equals(userId, that.userId)
                && Objects.equals(sourceId, that.sourceId)
                && Objects.equals(stateDate, that.stateDate)
                && Objects.equals(endDate, that.endDate)
                &&  Objects.equals(deviceType, that.deviceType)
                && Objects.equals(authorized, that.authorized)
                && Objects.equals(externalUserId, that.externalUserId);
    }

    @Override
    public int hashCode() {

        return Objects.hash(id, projectId, userId, sourceId, stateDate, endDate, deviceType,
                authorized, externalUserId);
    }

    @Override
    public String toString() {
        return "DevicePropertiesDTO{"
                + "id='" + id + '\''
                + ", projectId='" + projectId + '\''
                + ", userId='" + userId + '\''
                + ", sourceId='" + sourceId + '\''
                + ", stateDate=" + stateDate + '\''
                + ", endDate=" + endDate + '\''
                + ", deviceType=" + deviceType + '\''
                + ", authorized=" + authorized + '\''
                + ", externalUserId='" + externalUserId + '\''
                + '}';
    }



}
