package org.radarbase.authorizer.service.dto.managementportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subject {

  Project project;

  String subjectId;

  public Project getProject() {
    return project;
  }

  public String getSubjectId() {
    return subjectId;
  }
}
