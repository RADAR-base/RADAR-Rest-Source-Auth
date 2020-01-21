package org.radarbase.authorizer.config;

import java.util.Objects;
import javax.validation.constraints.NotNull;

public class ManagementPortalProperties {

  @NotNull
  private String baseUrl;

  @NotNull
  private String projectsPath;

  @NotNull
  private String subjectsPath;

  @NotNull
  private String oauthClientId;

  @NotNull
  private String oauthClientSecret;

  @NotNull
  private String tokenPath;

  public String getTokenPath() {
    return tokenPath;
  }

  public void setTokenPath(String tokenPath) {
    this.tokenPath = tokenPath;
  }

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

  @Override
  public String toString() {
    return "ManagementPortalProperties{" +
        "baseUrl='" + baseUrl + '\'' +
        ", projectsPath='" + projectsPath + '\'' +
        ", subjectsPath='" + subjectsPath + '\'' +
        ", oauthClientId='" + oauthClientId + '\'' +
        ", oauthClientSecret='" + oauthClientSecret + '\'' +
        ", tokenPath='" + tokenPath + '\'' +
        '}';
  }
}
