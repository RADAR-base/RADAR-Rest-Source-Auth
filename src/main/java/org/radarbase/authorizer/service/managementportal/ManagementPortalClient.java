package org.radarbase.authorizer.service.managementportal;

import java.util.Collection;
import org.radarbase.authorizer.service.dto.managementportal.Project;
import org.radarbase.authorizer.service.dto.managementportal.Subject;

public interface ManagementPortalClient<S extends Subject, P extends Project> {

  S getSubject(String subjectId);

  P getProject(String projectId);

  Collection<? extends Subject> getAllSubjects();

  Collection<? extends Project> getAllProjects();
}
