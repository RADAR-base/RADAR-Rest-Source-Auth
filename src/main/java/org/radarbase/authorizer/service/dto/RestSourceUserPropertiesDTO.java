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

package org.radarbase.authorizer.service.dto;


import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

import org.radarbase.authorizer.domain.RestSourceUser;


public class RestSourceUserPropertiesDTO implements Serializable {

    private static final long serialVersionUID = 1L;
    // Unique user key
    private String id;

    // Version to reset the user
    private String version = null;

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

    private String sourceType;

    // is authorized by user
    private Boolean authorized = false;

    private String externalUserId;

    public RestSourceUserPropertiesDTO() {
    }

    public RestSourceUserPropertiesDTO(RestSourceUser restSourceUser) {
        this.id = String.valueOf(restSourceUser.getId());
        this.projectId = restSourceUser.getProjectId();
        this.userId = restSourceUser.getUserId();
        this.sourceId = restSourceUser.getSourceId();
        this.authorized = restSourceUser.getAuthorized();
        this.sourceType = restSourceUser.getSourceType();
        this.endDate = restSourceUser.getEndDate();
        this.startDate = restSourceUser.getStartDate();
        this.externalUserId = restSourceUser.getExternalUserId();
        this.version = restSourceUser.getVersion();
    }

    public String getId() {
        return id;
    }

    public RestSourceUserPropertiesDTO id(String id) {
        this.id = id;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public RestSourceUserPropertiesDTO version(String version) {
        this.version = version;
        return this;
    }

    public String getProjectId() {
        return projectId;
    }

    public RestSourceUserPropertiesDTO projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public RestSourceUserPropertiesDTO userId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public RestSourceUserPropertiesDTO sourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public RestSourceUserPropertiesDTO startDate(Instant stateDate) {
        this.startDate = stateDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public RestSourceUserPropertiesDTO endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }


    public String getSourceType() {
        return sourceType;
    }

    public RestSourceUserPropertiesDTO sourceType(String sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public Boolean isAuthorized() {
        return authorized;
    }

    public RestSourceUserPropertiesDTO authorized(Boolean isAuthorized) {
        this.authorized = isAuthorized;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public RestSourceUserPropertiesDTO externalDeviceId(String externalDeviceId) {
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
        RestSourceUserPropertiesDTO that = (RestSourceUserPropertiesDTO) o;
        return Objects.equals(id, that.id) && Objects.equals(projectId, that.projectId)
                && Objects.equals(userId, that.userId)
                && Objects.equals(sourceId, that.sourceId)
                && Objects.equals(startDate, that.startDate)
                && Objects.equals(endDate, that.endDate)
                && Objects.equals(sourceType, that.sourceType)
                && Objects.equals(authorized, that.authorized)
                && Objects.equals(externalUserId, that.externalUserId);
    }

    @Override
    public int hashCode() {

        return Objects
                .hash(id, projectId, userId, sourceId, startDate, endDate, sourceType, authorized,
                        externalUserId);
    }

    @Override
    public String toString() {
        return "RestSourceUserPropertiesDTO{"
                + "id='" + id + '\''
                + ", projectId='" + projectId + '\''
                + ", userId='" + userId + '\''
                + ", sourceId='" + sourceId + '\''
                + ", startDate=" + startDate + '\''
                + ", endDate=" + endDate + '\''
                + ", sourceType=" + sourceType + '\''
                + ", authorized=" + authorized + '\''
                + ", externalUserId='" + externalUserId + '\''
                + '}';
    }


}
