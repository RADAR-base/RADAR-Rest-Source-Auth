package org.radarbase.authorizer.service.managementportal;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import javax.annotation.PostConstruct;
import javax.naming.ConfigurationException;
import javax.validation.constraints.NotNull;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.radarbase.authorizer.config.ManagementPortalProperties;
import org.radarbase.authorizer.service.dto.managementportal.Project;
import org.radarbase.authorizer.service.dto.managementportal.Subject;
import org.radarcns.exception.TokenException;
import org.radarcns.oauth.OAuth2Client;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "validator", havingValue = "managementportal")
public class ManagementPortalClient {

  private static final Logger LOGGER = LoggerFactory.getLogger(ManagementPortalClient.class);
  private Set<Subject> subjects;
  private Set<Project> projects;

  private Duration expiry = Duration.ofMinutes(20);
  private Instant lastFetch;

  private OkHttpClient httpClient;

  private OAuth2Client oAuth2Client;

  @Value("${mp-config-file-path}")
  private String mpConfigFilePath = "app-includes/management_portal_config.yml";

  private ManagementPortalProperties properties;

  private ObjectMapper mapper = new ObjectMapper();

  public ManagementPortalClient() {
  }

  public ManagementPortalClient(Duration expiry) {
    this.expiry = expiry;
  }

  @PostConstruct
  private void init() throws ConfigurationException, MalformedURLException {
    subjects = new HashSet<>();
    projects = new HashSet<>();

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
    return getProject(projectId) != null;
  }

  public boolean subjectExists(String subjectId) {
    return getSubject(subjectId) != null;
  }

  public boolean subjectExistsInProject(String subjectId, String projectId) {
    Subject subject = getSubject(subjectId);
    if (subject != null) {
      return subject.getProject().getProjectId().equals(projectId);
    } else {
      return false;
    }
  }

  public Subject getSubject(String subjectId) {
    // First check if need to refresh subjects cache
    if (isUpdateRequired()) {
      update();
      return this.subjects.stream().filter(subject1 -> subject1.getSubjectId().equals(subjectId))
          .findFirst().orElse(null);
    }

    // Try to find the subject in cache
    Optional<Subject> subject = this.subjects.stream()
        .filter(subject1 -> subject1.getSubjectId().equals(subjectId)).findFirst();

    // Try to get from MP if not present in cache.
    return subject.orElseGet(() -> {
      Subject subject1 = querySubject(subjectId);
      this.subjects.add(subject1);
      return subject1;
    });
  }

  public Project getProject(String projectId) {
    if (isUpdateRequired()) {
      update();
      return this.projects.stream()
          .filter(project1 -> project1.getProjectId().equals(projectId)).findFirst().orElse(null);
    }
    Optional<Project> project = this.projects.stream()
        .filter(project1 -> project1.getProjectId().equals(projectId)).findFirst();

    return project.orElseGet(() -> {
          Project project1 = queryProject(projectId);
          this.projects.add(project1);
          return project1;
        }
    );
  }

  public Stream<Subject> getAllSubjects() {
    if (isUpdateRequired()) {
      update();
    }
    return this.subjects.stream();
  }

  public Stream<Project> getAllProjects() {
    if (isUpdateRequired()) {
      update();
    }
    return this.projects.stream();
  }

  private Subject querySubject(String subjectId) {
    return queryEntity(
        properties.getBaseUrl() + properties.getSubjectsPath() + "/" + subjectId,
        new TypeReference<Subject>() {
        });
  }

  private Project queryProject(String projectId) {
    return queryEntity(
        properties.getBaseUrl() + properties.getProjectsPath() + "/" + projectId,
        new TypeReference<Project>() {
        });
  }

  private <T> T queryEntity(String url, TypeReference<T> t) {
    try {
      Request request = new Request.Builder()
          .addHeader("Authorization", "Bearer " + oAuth2Client.getValidToken())
          .url(new URL(url))
          .get()
          .build();
      Response response = httpClient.newCall(request).execute();
      if (response.isSuccessful() && response.body() != null) {
        return mapper.readValue(response.body().string(), t);
      } else {
        return null;
      }
    } catch (TokenException exc) {
      LOGGER.warn("Cannot get a valid token from Management Portal.", exc);
      return null;
    } catch (MalformedURLException exc) {
      LOGGER.warn("URL is mis-configured.", exc);
      return null;
    } catch (IOException exc) {
      LOGGER.warn("An error occurred while making HTTP request for {}.", t.getType().getTypeName(),
          exc);
      return null;
    }
  }

  private Set<Subject> queryAllSubjects() {
    // get subjects from MP
    return queryEntity(properties.getBaseUrl() + properties.getSubjectsPath(),
        new TypeReference<Set<Subject>>() {
        });
  }

  private Set<Project> queryAllProjects() {
    // get projects from MP
    return queryEntity(properties.getBaseUrl() + properties.getProjectsPath(),
        new TypeReference<Set<Project>>() {
        });
  }

  synchronized private void update() {
    this.subjects = queryAllSubjects();
    this.projects = queryAllProjects();
    lastFetch = Instant.now();
  }

  synchronized private boolean isUpdateRequired() {
    return this.lastFetch.plus(this.expiry).isBefore(Instant.now());
  }
}
