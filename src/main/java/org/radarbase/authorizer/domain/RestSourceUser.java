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

package org.radarbase.authorizer.domain;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;

import lombok.Data;
import org.radarbase.authorizer.service.dto.RestSourceAccessToken;
import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;

@Data
@Entity
@Table(name = "rest_source_user")
public class RestSourceUser {

    @Transient
    private static final Duration EXPIRY_TIME_MARGIN = Duration.ofMinutes(5);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    // The version to be appended to ID for RESET of a user
    // This should be updated whenever the user is RESET.
    // By default this is null for backwards compatibility
    private String version = null;

    // The number of times a user has been reset
    private long timesReset = 0;

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

    private String accessToken;

    private String refreshToken;

    private Integer expiresIn;

    private Instant expiresAt;

    private String tokenType;

    public RestSourceUser() {
        if (this.sourceId == null) {
            this.sourceId = UUID.randomUUID().toString();
        }
    }

    public RestSourceUser(RestSourceUserPropertiesDTO restSourceUserPropertiesDTO) {
        if (restSourceUserPropertiesDTO.getId() != null) {
            this.id = Long.valueOf(restSourceUserPropertiesDTO.getId());
        }
        this.projectId = restSourceUserPropertiesDTO.getProjectId();
        this.userId = restSourceUserPropertiesDTO.getUserId();
        this.sourceId = restSourceUserPropertiesDTO.getSourceId();
        this.sourceType = restSourceUserPropertiesDTO.getSourceType();
        this.startDate = restSourceUserPropertiesDTO.getStartDate();
        this.endDate = restSourceUserPropertiesDTO.getEndDate();
        this.externalUserId = restSourceUserPropertiesDTO.getExternalUserId();
        this.authorized = restSourceUserPropertiesDTO.isAuthorized();
    }

    public long getId() {
        return id;
    }

    public RestSourceUser id(Long id) {
        this.id = id;
        return this;
    }

    public String getVersion() {
        return version;
    }

    public RestSourceUser version(String version) {
        this.version = version;
        return this;
    }

      public long getTimesReset() {
      return timesReset;
    }

    public RestSourceUser setTimesReset(long timesReset) {
      this.timesReset = timesReset;
      return this;
    }

  public String getProjectId() {
        return projectId;
    }

    public RestSourceUser projectId(String projectId) {
        this.projectId = projectId;
        return this;
    }

    public String getUserId() {
        return userId;
    }

    public RestSourceUser userId(String userId) {
        this.userId = userId;
        return this;
    }

    public String getSourceId() {
        return sourceId;
    }

    public RestSourceUser sourceId(String sourceId) {
        this.sourceId = sourceId;
        return this;
    }

    public Instant getStartDate() {
        return startDate;
    }

    public RestSourceUser startDate(Instant startDate) {
        this.startDate = startDate;
        return this;
    }

    public Instant getEndDate() {
        return endDate;
    }

    public RestSourceUser endDate(Instant endDate) {
        this.endDate = endDate;
        return this;
    }

    public String getSourceType() {
        return sourceType;
    }

    public RestSourceUser sourceType(String sourceType) {
        this.sourceType = sourceType;
        return this;
    }

    public Boolean getAuthorized() {
        return authorized;
    }

    public RestSourceUser authorized(Boolean authorized) {
        this.authorized = authorized;
        return this;
    }

    public String getExternalUserId() {
        return externalUserId;
    }

    public RestSourceUser externalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
        return this;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public RestSourceUser accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public RestSourceUser refreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
        return this;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public RestSourceUser expiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public RestSourceUser expiresIn(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }

    public String getTokenType() {
        return tokenType;
    }

    public RestSourceUser tokenType(String tokenType) {
        this.tokenType = tokenType;
        return this;
    }

    /**
     * Updates only a subset of user properties during update.
     * Does not update properties such as id and token data.
     *
     * @param sourceUserDto user details to update.
     */
    public void safeUpdateProperties(RestSourceUserPropertiesDTO sourceUserDto) {
        this.projectId = sourceUserDto.getProjectId();
        this.userId = sourceUserDto.getUserId();
        this.sourceId = sourceUserDto.getSourceId();
        this.startDate = sourceUserDto.getStartDate();
        this.endDate = sourceUserDto.getEndDate();
    }


    /**
     * Updates only the token related properties during update.
     * Does not update properties such as id and study information.
     *
     * @param restSourceAccessToken token details to update.
     */
    public void safeUpdateTokenDetails(RestSourceAccessToken restSourceAccessToken) {
        this.accessToken = restSourceAccessToken.getAccessToken();
        this.refreshToken = restSourceAccessToken.getRefreshToken();
        this.expiresIn = restSourceAccessToken.getExpiresIn();
        this.expiresAt = Instant.now()
                .plusSeconds(restSourceAccessToken.getExpiresIn())
                .minus(EXPIRY_TIME_MARGIN);
    }
}
