package org.radarbase.authorizer.validation;

public interface Validator {

  boolean validateProject(String projectId);

  boolean validateSubject(String subjectId);

  boolean validateSubjectInProject(String subjectId, String projectId);
}
