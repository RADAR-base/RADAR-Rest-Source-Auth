package org.radarbase.authorizer.service.dto.managementportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {
  String projectId;

  public String getProjectId() {
    return projectId;
  }
}
