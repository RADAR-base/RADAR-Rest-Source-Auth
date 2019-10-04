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
import javax.annotation.PostConstruct;
import javax.validation.Valid;
import org.radarbase.authorizer.service.RestSourceUserService;
import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;
import org.radarbase.authorizer.service.dto.RestSourceUsers;
import org.radarbase.authorizer.service.dto.TokenDTO;
import org.radarbase.authorizer.validation.Validator;
import org.radarbase.authorizer.validation.ValidatorFactory;
import org.radarbase.authorizer.validation.exception.ValidationFailedException;
import org.radarbase.authorizer.validation.exception.ValidatorNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RestSourceUserResource {

  private Logger logger = LoggerFactory.getLogger(RestSourceUserResource.class);

  @Autowired
  private RestSourceUserService restSourceUserService;

  @Value("${validator}")
  private String validatorType = "";

  private Validator validator;

  @PostConstruct
  private void init() {
    try {
      this.validator = ValidatorFactory.getValidator(validatorType);
    } catch (ValidatorNotFoundException exc) {
      logger.warn("No Valid Validator Found. Will not be validating requests...");
      this.validator = null;
    } catch (Exception exc) {
      logger.warn("There was an error when initialising the validator {}.", validatorType, exc);
      this.validator = null;
    }
  }

  @PostMapping("/users")
  public ResponseEntity addAuthorizedRestSourceUser(@RequestParam(value = "code") String code,
      @RequestParam(value = "state") String state) throws URISyntaxException {
    logger.debug("Add a rest-source user with code {} and state {}", code, state);
    RestSourceUserPropertiesDTO
        user = this.restSourceUserService.authorizeAndStoreDevice(code, state);
    return ResponseEntity
        .created(new URI("/user/" + user.getId())).body(user);
  }

  @GetMapping("/users")
  public ResponseEntity<RestSourceUsers> getAllRestSources(
      @RequestParam(value = "source-type", required = false) String sourceType) {
    if (sourceType != null && !sourceType.isEmpty()) {
      logger.debug("Get all rest source users by type {}", sourceType);
      return ResponseEntity
          .ok(this.restSourceUserService.getAllUsersBySourceType(sourceType));
    }

    logger.debug("Get all rest source users");
    return ResponseEntity
        .ok(this.restSourceUserService.getAllRestSourceUsers());
  }

  @GetMapping("/users/{id}")
  public ResponseEntity<RestSourceUserPropertiesDTO> getRestSourceUserById(
      @PathVariable String id) {
    logger.debug("Get rest source user with id {}", id);
    return ResponseEntity
        .ok(this.restSourceUserService.getRestSourceUserById(Long.valueOf(id)));
  }

  @PostMapping("/users/{id}")
  public ResponseEntity updateDeviceUser(@Valid @PathVariable String id,
      @RequestBody RestSourceUserPropertiesDTO restSourceUser,
      @RequestParam(value = "validate", defaultValue = "false") Boolean isValidate) {
    logger.debug("Requesting to update rest source user");
    if (validator != null && isValidate && !validator
        .validateSubjectInProject(restSourceUser.getUserId(), restSourceUser.getProjectId())) {
      throw new ValidationFailedException(restSourceUser, validator);
    }
    return ResponseEntity
        .ok(this.restSourceUserService.updateRestSourceUser(Long.valueOf(id), restSourceUser));
  }

  @DeleteMapping("/users/{id}")
  public ResponseEntity<Void> deleteDeviceUser(@Valid @PathVariable String id) {
    logger.debug("Requesting to delete rest source user");
    this.restSourceUserService.revokeTokenAndDeleteUser(Long.valueOf(id));
    return ResponseEntity
        .ok().header("user-removed", id).build();
  }


  @GetMapping("/users/{id}/token")
  public ResponseEntity<TokenDTO> getUserToken(@PathVariable String id) {
    logger.debug("Get user token for rest source user id {}", id);
    return ResponseEntity
        .ok(this.restSourceUserService.getDeviceTokenByUserId(Long.valueOf(id)));
  }

  @PostMapping("/users/{id}/token")
  public ResponseEntity<TokenDTO> requestRefreshTokenForUser(@PathVariable String id) {
    logger.debug("Refreshing user token for rest source user id {}", id);
    return ResponseEntity
        .ok(this.restSourceUserService.refreshTokenForUser(Long.valueOf(id)));
  }
}