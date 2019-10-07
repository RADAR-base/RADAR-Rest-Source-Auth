package org.radarbase.authorizer.validation;

import org.radarbase.authorizer.service.dto.RestSourceUserPropertiesDTO;
import org.radarbase.authorizer.service.dto.managementportal.Subject;
import org.radarbase.authorizer.service.managementportal.ManagementPortalClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "validator", havingValue = "managementportal")
public class ManagementPortalValidator implements Validator {

  private static final Logger logger = LoggerFactory.getLogger(ManagementPortalValidator.class);
  private final ManagementPortalClient mpClient;

  @Autowired
  public ManagementPortalValidator(ManagementPortalClient mpClient) {
    this.mpClient = mpClient;
  }

  @Override
  public boolean validate(RestSourceUserPropertiesDTO restSourceUser) {
    if (restSourceUser == null) {
      return false;
    }

    Subject subject = mpClient.getSubject(restSourceUser.getUserId());
    if (subject != null) {
      return subject.getProject().getProjectId().equals(restSourceUser.getProjectId());
    } else {
      logger.warn("The subject with id {} was not found in the Management portal.",
          restSourceUser.getUserId());
      return false;
    }
  }
}
