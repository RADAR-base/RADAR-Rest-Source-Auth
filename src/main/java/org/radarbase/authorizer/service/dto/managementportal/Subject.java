package org.radarbase.authorizer.service.dto.managementportal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Subject {

  private Project project;

  private String subjectId;

  public Project getProject() {
    return project;
  }

  public String getSubjectId() {
    return subjectId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Subject subject = (Subject) o;
    return project.equals(subject.project) &&
        subjectId.equals(subject.subjectId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(project, subjectId);
  }
}
