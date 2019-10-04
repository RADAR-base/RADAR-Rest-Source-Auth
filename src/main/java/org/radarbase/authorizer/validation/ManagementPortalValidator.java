package org.radarbase.authorizer.validation;

import org.radarbase.authorizer.service.managementportal.ManagementPortalClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

@ConditionalOnProperty(value = "validator", havingValue = "managementportal")
public class ManagementPortalValidator implements Validator {

  private static final ManagementPortalClient mpClient = new ManagementPortalClient();

  @Override
  public boolean validateProject(String projectId) {
    return mpClient.projectExists(projectId);
  }

  @Override
  public boolean validateSubject(String subjectId) {
    return mpClient.subjectExists(subjectId);
  }

  @Override
  public boolean validateSubjectInProject(String subjectId, String projectId) {
    return mpClient.subjectExistsInProject(subjectId, projectId);
  }
}
