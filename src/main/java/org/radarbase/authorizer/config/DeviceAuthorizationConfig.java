package org.radarbase.authorizer.config;

import java.util.Objects;

public class DeviceAuthorizationConfig {

    private String deviceType;

    private String authorizationEndpoint;

    private String tokenEndpoint;

    private String grantType;

    private String scope;

    private String clientId;

    private String clientSecret;

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceAuthorizationConfig that = (DeviceAuthorizationConfig) o;
        return Objects.equals(deviceType, that.deviceType) && Objects
                .equals(authorizationEndpoint, that.authorizationEndpoint) && Objects
                .equals(tokenEndpoint, that.tokenEndpoint) && Objects
                .equals(grantType, that.grantType) && Objects.equals(scope, that.scope) && Objects
                .equals(clientId, that.clientId) && Objects.equals(clientSecret, that.clientSecret);
    }

    @Override
    public int hashCode() {

        return Objects
                .hash(deviceType, authorizationEndpoint, tokenEndpoint, grantType, scope, clientId,
                        clientSecret);
    }
}
