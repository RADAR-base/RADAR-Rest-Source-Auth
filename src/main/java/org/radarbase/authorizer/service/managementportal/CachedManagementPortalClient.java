package org.radarbase.authorizer.service.managementportal;

import static org.radarbase.authorizer.validation.ManagementPortalValidator.MP_VALIDATOR_PROPERTY_VALUE;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.naming.ConfigurationException;
import javax.validation.constraints.NotNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.radarbase.authorizer.config.ConfigHelper;
import org.radarbase.authorizer.config.ManagementPortalProperties;
import org.radarbase.authorizer.service.dto.managementportal.Project;
import org.radarbase.authorizer.service.dto.managementportal.Subject;
import org.radarcns.exception.TokenException;
import org.radarcns.oauth.OAuth2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "validator", havingValue = MP_VALIDATOR_PROPERTY_VALUE)
public class CachedManagementPortalClient implements ManagementPortalClient<Subject, Project> {

  private static final Logger LOGGER = LoggerFactory.getLogger(CachedManagementPortalClient.class);
  private Set<Subject> subjects;
  private Set<Project> projects;

  private Duration expiry = Duration.ofHours(1);
  private Instant lastFetch;

  private OkHttpClient httpClient;

  private OAuth2Client oAuth2Client;

  private ManagementPortalProperties properties;

  private ObjectMapper mapper = new ObjectMapper();

  @Autowired
  public CachedManagementPortalClient(@Value("${mp-config-file-path}") String mpConfigFilePath)
      throws MalformedURLException, ConfigurationException {
    subjects = new HashSet<>();
    projects = new HashSet<>();
    lastFetch = Instant.MIN;
    init(mpConfigFilePath);
  }

  public CachedManagementPortalClient(@Value("${mp-config-file-path}") String mpConfigFilePath,
      Duration expiry) throws MalformedURLException, ConfigurationException {
    this.expiry = expiry;
    subjects = new HashSet<>();
    projects = new HashSet<>();
    lastFetch = Instant.MIN;
    init(mpConfigFilePath);
  }

  private void init(String mpConfigFilePath) throws ConfigurationException, MalformedURLException {

    this.httpClient = new OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .readTimeout(50, TimeUnit.SECONDS)
        .build();

    this.mapper = new ObjectMapper()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    this.properties = loadManagementPortalProperties(mpConfigFilePath);
    this.oAuth2Client = new OAuth2Client.Builder()
        .credentials(properties.getOauthClientId(), properties.getOauthClientSecret())
        .endpoint(new URL(properties.getBaseUrl()), "/oauth/token")
        .httpClient(httpClient)
        .build();
  }

  private ManagementPortalProperties loadManagementPortalProperties(@NotNull String path)
      throws ConfigurationException {
    ManagementPortalProperties properties = ConfigHelper
        .loadPropertiesFromFile(path, new TypeReference<ManagementPortalProperties>() {
        });

    if (properties.anyNull()) {
      throw new ConfigurationException(
          "Some of the required properties were not defined in " + path);
    }

    return properties;
  }

  @Override
  public Subject getSubject(String subjectId) throws IOException, TokenException {
    // First check if need to refresh subjects cache
    if (isUpdateRequired()) {
      update();
      return this.subjects.stream()
          .filter(subject1 -> subject1.getSubjectId().equals(subjectId))
          .findFirst()
          .orElse(null);
    }

    // Try to find the subject in cache if not updated
    Optional<Subject> subject = this.subjects.stream()
        .filter(subject1 -> subject1.getSubjectId().equals(subjectId))
        .findFirst();

    // Try to get from MP if not present in cache.
    return subject.orElse(querySubject(subjectId));
  }

  @Override
  public Project getProject(String projectId) throws IOException, TokenException {
    if (isUpdateRequired()) {
      update();
      return this.projects.stream()
          .filter(project1 -> project1.getProjectId().equals(projectId))
          .findFirst()
          .orElse(null);
    }
    Optional<Project> project = this.projects.stream()
        .filter(project1 -> project1.getProjectId().equals(projectId))
        .findFirst();

    return project.orElse(queryProject(projectId));
  }

  @Override
  public Set<Subject> getAllSubjects() throws IOException, TokenException {
    if (isUpdateRequired()) {
      update();
    }
    return this.subjects;
  }

  @Override
  public Set<Project> getAllProjects() throws IOException, TokenException {
    if (isUpdateRequired()) {
      update();
    }
    return this.projects;
  }

  private Subject querySubject(String subjectId) throws IOException, TokenException {
    Subject subject = queryEntity(
        properties.getBaseUrl() + "/api" + properties.getSubjectsPath() + "/" + subjectId,
        new TypeReference<Subject>() {
        });
    this.subjects.add(subject);
    return subject;
  }

  private Project queryProject(String projectId) throws IOException, TokenException {
    Project project = queryEntity(
        properties.getBaseUrl() + "/api" + properties.getProjectsPath() + "/" + projectId,
        new TypeReference<Project>() {
        });

    this.projects.add(project);
    return project;
  }

  private <T> T queryEntity(String url, TypeReference<T> t)
      throws TokenException, IOException {
    Request request = new Request.Builder()
        .addHeader("Authorization", "Bearer " + oAuth2Client.getValidToken().getAccessToken())
        .url(new URL(url))
        .get()
        .build();
    Response response = httpClient.newCall(request).execute();
    if (response.isSuccessful() && response.body() != null) {
      return mapper.readValue(response.body().string(), t);
    } else {
      throw new IOException(
          "The Request was not successful: Status-" + response.code() + ", Body-" +
              (response.body() != null ? response.body().string() : ""));
    }
  }

  private Set<Subject> queryAllSubjects() throws IOException, TokenException {
    // get subjects from MP
    return queryEntity(properties.getBaseUrl() + "/api" + properties.getSubjectsPath(),
        new TypeReference<Set<Subject>>() {
        });
  }

  private Set<Project> queryAllProjects() throws IOException, TokenException {
    // get projects from MP
    return queryEntity(properties.getBaseUrl() + "/api" + properties.getProjectsPath(),
        new TypeReference<Set<Project>>() {
        });
  }

  synchronized private void update() throws IOException, TokenException {
    Set<Subject> subjects1 = queryAllSubjects();
    Set<Project> projects1 = queryAllProjects();
    this.subjects = subjects1 == null ? new HashSet<>() : subjects1;
    this.projects = projects1 == null ? new HashSet<>() : projects1;
    lastFetch = Instant.now();
  }

  synchronized private boolean isUpdateRequired() {
    return this.lastFetch.plus(this.expiry).isBefore(Instant.now());
  }
}
