package org.radarbase.authorizer.config;

import java.util.Objects;

public class ManagementPortalProperties {

  private String baseUrl;

  private String projectsPath;

  private String subjectsPath;

  private String oauthClientId;

  private String oauthClientSecret;

  public String getBaseUrl() {
    return baseUrl;
  }

  public void setBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
  }

  public String getProjectsPath() {
    return projectsPath;
  }

  public void setProjectsPath(String projectsPath) {
    this.projectsPath = projectsPath;
  }

  public String getSubjectsPath() {
    return subjectsPath;
  }

  public void setSubjectsPath(String subjectsPath) {
    this.subjectsPath = subjectsPath;
  }

  public String getOauthClientId() {
    return oauthClientId;
  }

  public void setOauthClientId(String oauthClientId) {
    this.oauthClientId = oauthClientId;
  }

  public String getOauthClientSecret() {
    return oauthClientSecret;
  }

  public void setOauthClientSecret(String oauthClientSecret) {
    this.oauthClientSecret = oauthClientSecret;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ManagementPortalProperties that = (ManagementPortalProperties) o;
    return baseUrl.equals(that.baseUrl) &&
        projectsPath.equals(that.projectsPath) &&
        subjectsPath.equals(that.subjectsPath) &&
        oauthClientId.equals(that.oauthClientId) &&
        oauthClientSecret.equals(that.oauthClientSecret);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseUrl, projectsPath, subjectsPath, oauthClientId, oauthClientSecret);
  }

  public boolean anyNull() {
    return baseUrl == null || projectsPath == null || subjectsPath == null || oauthClientId == null
        || oauthClientSecret == null;
  }
}
