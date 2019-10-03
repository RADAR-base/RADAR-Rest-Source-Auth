package org.radarbase.authorizer.service.managementportal;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;
import javax.validation.constraints.NotNull;
import org.radarbase.authorizer.config.ManagementPortalProperties;
import org.radarbase.authorizer.service.dto.managementportal.Project;
import org.radarbase.authorizer.service.dto.managementportal.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ManagementPortalClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPortalClient.class);
  private Stream<Subject> subjects;
  private Stream<Project> projects;

  private Duration expiry = Duration.ofMinutes(2);
  private Instant lastFetch;

  @Value("${mp-config-file-path}")
  private String mpConfigFilePath = "app-includes/management_portal_config.yml";

  public ManagementPortalClient() {
  }

  public ManagementPortalClient(Duration expiry) {
    this.expiry = expiry;
  }

  @PostConstruct
  private void init() throws ConfigurationException {
    // Read properties and initialise the client
    // copy paste from the appserver integration tests.
    ManagementPortalProperties properties = loadManagementPortalProperties(mpConfigFilePath);
    lastFetch = Instant.MIN;
  }

  private ManagementPortalProperties loadManagementPortalProperties(@NotNull String path) throws
      ConfigurationException {
    LOGGER.info("Loading management portal config from {}", path);
    YAMLFactory yamlFactory = new YAMLFactory();
    try {
      YAMLParser yamlParser = yamlFactory.createParser(new File(path));
      ManagementPortalProperties managementPortalProperties = new ObjectMapper(yamlFactory)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
          .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE)
          .readValue(yamlParser, ManagementPortalProperties.class);

      if (managementPortalProperties == null || managementPortalProperties.anyNull()) {
        LOGGER.error("No valid configurations available on configured path. Please "
            + "check the syntax and file name");
        throw new ConfigurationException(
            "No valid management portal configs are provided" + ".");
      }
      return managementPortalProperties;
    } catch (IOException e) {
      LOGGER.error("Could not successfully read config file at {}", path);
      throw new ConfigurationException("Could not successfully read config file at " + path);
    }

  }

  public boolean projectExists(String projectId) {
    updateIfExpired();
    return this.projects.anyMatch(project -> project.getProjectId().equals(projectId));
  }

  public boolean subjectExists(String subjectId) {
    updateIfExpired();
    return this.subjects.anyMatch(subject -> subject.getSubjectId().equals(subjectId));
  }

  public boolean subjectExistsInProject(String subjectId, String projectId) {
    updateIfExpired();
    return this.subjects.anyMatch(
        subject -> subject.getSubjectId().equals(subjectId) && subject.getProject().getProjectId()
            .equals(projectId));
  }

  public Subject getSubject(String subjectId) {
    Optional<Subject> subject = this.subjects
        .filter(subject1 -> subject1.getSubjectId().equals(subjectId)).findFirst();

    if (subject.isEmpty()) {
      // get subject from MP
      return null;
    } else {
      return subject.get();
    }
  }

  public Project getProject(String projectId) {
    Optional<Project> project = this.projects
        .filter(project1 -> project1.getProjectId().equals(projectId)).findFirst();

    if (project.isEmpty()) {
      // get project from MP
      return null;
    } else {
      return project.get();
    }
  }

  public Stream<Subject> getAllSubjects() {
    return null;
  }

  public Stream<Project> getAllProjects() {
    return null;
  }

  private void updateIfExpired() {
    if (lastFetch.plus(expiry).isBefore(Instant.now())) {
      synchronized (this) {
        this.subjects = getAllSubjects();
        this.projects = getAllProjects();
        lastFetch = Instant.now();
      }
    }
  }
}
