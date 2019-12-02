package org.radarbase.authorizer.service.managementportal;

import java.io.IOException;
import java.util.Collection;
import org.radarbase.authorizer.service.dto.managementportal.Project;
import org.radarbase.authorizer.service.dto.managementportal.Subject;
import org.radarcns.exception.TokenException;

public interface ManagementPortalClient<S extends Subject, P extends Project> {

  S getSubject(String subjectId) throws IOException, TokenException;

  P getProject(String projectId) throws IOException, TokenException;

  Collection<S> getAllSubjects() throws IOException, TokenException;

  Collection<P> getAllProjects() throws IOException, TokenException;
}
