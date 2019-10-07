package org.radarbase.authorizer.service.managementportal;

import java.util.Collection;
import org.radarbase.authorizer.service.dto.managementportal.Project;
import org.radarbase.authorizer.service.dto.managementportal.Subject;

public interface ManagementPortalClient {

  <T extends Subject> T getSubject(String subjectId);

  <T extends Project> T getProject(String projectId);

  Collection<? extends Subject> getAllSubjects();

  Collection<? extends Project> getAllProjects();
}
