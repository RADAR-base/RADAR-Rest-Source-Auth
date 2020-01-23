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

package org.radarbase.authorizer.service;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import org.radarbase.authorizer.domain.RestSourceUser;
import org.radarbase.authorizer.repository.RestSourceUserRepository;
import org.radarbase.authorizer.service.dto.RestSourceAccessToken;
import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;
import org.radarbase.authorizer.service.dto.RestSourceUsers;
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
public class RestSourceUserService {

    private final Logger log = LoggerFactory.getLogger(RestSourceUserService.class);

    @Autowired
    private RestSourceUserRepository restSourceUserRepository;

    @Autowired
    private RestSourceClientService authorizationService;

    @Transactional(readOnly = true)
    public RestSourceUsers getAllRestSourceUsers() {
        log.debug("Querying all saved source users");
        return new RestSourceUsers()
            .users(this.restSourceUserRepository.findAll()
                .stream()
                .map(RestSourceUserPropertiesDTO::new)
                .collect(Collectors.toList()));
    }

    public RestSourceUserPropertiesDTO save(RestSourceUserPropertiesDTO restSourceUserPropertiesDTO) {
      RestSourceUser restSourceUser =
          this.restSourceUserRepository.save(new RestSourceUser(restSourceUserPropertiesDTO));
      return new RestSourceUserPropertiesDTO(restSourceUser);
    }

    @Transactional
    public RestSourceUserPropertiesDTO authorizeAndStoreDevice(@NotNull String code,
        @NotNull String sourceType) {
      RestSourceAccessToken accessToken =
          authorizationService.getAccessTokenWithAuthorizeCode(code, sourceType);

      if (accessToken != null) {

        Optional<RestSourceUser> existingUser = restSourceUserRepository
            .findBySourceTypeAndExternalUserId(sourceType, accessToken.getExternalUserId());

        RestSourceUser resultUser;
        if (existingUser.isPresent()) {
          resultUser = existingUser.get();
          resultUser.safeUpdateTokenDetails(accessToken);
        } else {
          resultUser = new RestSourceUser()
              .authorized(true)
              .externalUserId(accessToken.getExternalUserId())
              .sourceType(sourceType)
              .startDate(Instant.now());
          resultUser.safeUpdateTokenDetails(accessToken);

          resultUser = this.restSourceUserRepository.save(resultUser);
        }
        return new RestSourceUserPropertiesDTO(resultUser);
      } else {
        throw new TokenException();
      }
    }

    @Transactional(readOnly = true)
    public RestSourceUserPropertiesDTO getRestSourceUserById(Long id) {
      Optional<RestSourceUser> user = restSourceUserRepository.findById(id);

      if (user.isPresent()) {
        return new RestSourceUserPropertiesDTO(user.get());
      } else {
        throw new NotFoundException("RestSourceUser not found with id " + id);
      }
    }

    @Transactional
    public RestSourceUserPropertiesDTO updateRestSourceUser(Long id,
        RestSourceUserPropertiesDTO sourceUserPropertiesDTO) {

      Optional<RestSourceUser> sourceUser = restSourceUserRepository.findById(id);

      if (sourceUser.isPresent()) {
        RestSourceUser restSourceUserToSave = sourceUser.get();
        restSourceUserToSave.safeUpdateProperties(sourceUserPropertiesDTO);
        return new RestSourceUserPropertiesDTO(restSourceUserRepository.save(restSourceUserToSave));
      } else {
        throw new NotFoundException(
            "Unable to update rest source user. RestSourceUser not found with " + "id "
                + sourceUserPropertiesDTO.getId());
      }

    }

    /**
     * Removes user from database and revokes access token and refresh token
     *
     * @param id userID
     */
    @Transactional
    public void revokeTokenAndDeleteUser(Long id) {
      Optional<RestSourceUser> user = restSourceUserRepository.findById(id);

      if (user.isPresent()) {
        RestSourceUser restSourceUser = user.get();
        authorizationService
            .revokeToken(restSourceUser.getAccessToken(), restSourceUser.getSourceType());
        restSourceUserRepository.deleteById(id);

      } else {
        throw new NotFoundException("RestSourceUser not found with id " + id);
      }
    }

    @Transactional(readOnly = true)
    public TokenDTO getDeviceTokenByUserId(Long id) {
      Optional<RestSourceUser> user = restSourceUserRepository.findById(id);

      if (user.isPresent()) {
        RestSourceUser restSourceUser = user.get();
        return new TokenDTO()
            .accessToken(restSourceUser.getAccessToken())
            .expiresAt(restSourceUser.getExpiresAt());
      } else {
        throw new NotFoundException("RestSourceUser not found with id " + id);
      }
    }

    @Transactional
    public TokenDTO refreshTokenForUser(Long id) {
      Optional<RestSourceUser> user = restSourceUserRepository.findById(id);
      if (user.isPresent()) {
        RestSourceUser restSourceUser = user.get();
        // refresh token by user id and source-type
        RestSourceAccessToken accessToken = authorizationService
            .refreshToken(restSourceUser.getRefreshToken(), restSourceUser.getSourceType());
        // update token
        if (accessToken != null) {
          restSourceUser.safeUpdateTokenDetails(accessToken);
          restSourceUser = this.restSourceUserRepository.save(restSourceUser);
          return new TokenDTO()
              .accessToken(restSourceUser.getAccessToken())
              .expiresAt(restSourceUser.getExpiresAt());
        } else {
          throw new TokenException("Could not refresh token successfully");
        }

      } else {
        throw new NotFoundException("RestSourceUser not found with id " + id);
      }
    }

    @Transactional(readOnly = true)
    public RestSourceUsers getAllUsersBySourceType(String sourceType) {
      log.debug("Querying all saved users by source-type {}", sourceType);
      return new RestSourceUsers()
          .users(this.restSourceUserRepository.findAllBySourceType(sourceType)
              .stream()
              .map(RestSourceUserPropertiesDTO::new)
              .collect(Collectors.toList()));
    }

    /**
     * This resets the user by updating the current version. Currently, the version is calculated as a
     * String based on the current time instant. This may change in the future.
     *
     * @param id the database ID of the User
     * @return The updated User details
     */
    public RestSourceUserPropertiesDTO resetUser(Long id) {
      Optional<RestSourceUser> sourceUser = restSourceUserRepository.findById(id);

      if (sourceUser.isPresent()) {
        RestSourceUser restSourceUserToSave = sourceUser.get();
        return new RestSourceUserPropertiesDTO(
            restSourceUserRepository.save(
                restSourceUserToSave
                    .version(Instant.now().toString())
                    .setTimesReset(restSourceUserToSave.getTimesReset() + 1)));
      } else {
        throw new NotFoundException(
            "Unable to reset rest source user. RestSourceUser not found with " + "id "
                + id);
      }
    }
}
