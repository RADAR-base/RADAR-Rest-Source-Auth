package org.radarbase.authorizer.service.dto.managementportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

  @JsonProperty("projectName")
  private String projectId;

  public String getProjectId() {
    return projectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Project project = (Project) o;
    return Objects.equals(projectId, project.projectId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectId);
  }
}
