package org.radarbase.authorizer.domain;

import java.time.Instant;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import org.radarbase.authorizer.service.dto.DevicePropertiesDTO;

@Data
@Entity
@Table(name = "radar_device")
public class Device {

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

    public Device() {}

    public Device(DevicePropertiesDTO devicePropertiesDTO) {
        if(devicePropertiesDTO.getId() != null) {
            this.id = devicePropertiesDTO.getId();
        }
        this.projectId = devicePropertiesDTO.getProjectId();
        this.userId = devicePropertiesDTO.getUserId();
        this.sourceId = devicePropertiesDTO.getSourceId();
        this.deviceType = devicePropertiesDTO.getDeviceType();
        this.startDate = devicePropertiesDTO.getStartDate();
        this.endDate = devicePropertiesDTO.getEndDate();
        this.externalUserId = devicePropertiesDTO.getExternalUserId();
        this.authorized = devicePropertiesDTO.isAuthorized();
    }

    public long getId() {
        return id;
    }

    public Device id(Long id) {
        this.id = id;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public Device projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public Device userId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public Device sourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public Device startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public Device endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public Device deviceType(String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public Device authorized(Boolean authorized) {
        this.authorized = authorized;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public Device externalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        return this;
    }
}
