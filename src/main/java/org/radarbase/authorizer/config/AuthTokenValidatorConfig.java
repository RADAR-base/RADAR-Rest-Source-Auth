package org.radarbase.authorizer.config;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

public class AuthTokenValidatorConfig implements org.radarcns.auth.config.TokenValidatorConfig {

    private List<URI> publicKeyEndpoints = new LinkedList<>();

    private String resourceName;

    private List<String> publicKeys = new LinkedList<>();

    @Override
    public List<URI> getPublicKeyEndpoints() {
        return publicKeyEndpoints;
    }

    @Override
    public String getResourceName() {
        return resourceName;
    }

    @Override
    public List<String> getPublicKeys() {
        return publicKeys;
    }

    public void setPublicKeyEndpoints(List<URI> publicKeyEndpoints) {
        this.publicKeyEndpoints = publicKeyEndpoints;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public void setPublicKeys(List<String> publicKeys) {
        this.publicKeys = publicKeys;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthTokenValidatorConfig that = (AuthTokenValidatorConfig) o;
        return Objects.equals(publicKeyEndpoints, that.publicKeyEndpoints) && Objects
                .equals(resourceName, that.resourceName) && Objects
                .equals(publicKeys, that.publicKeys);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicKeyEndpoints, resourceName, publicKeys);
    }
}
