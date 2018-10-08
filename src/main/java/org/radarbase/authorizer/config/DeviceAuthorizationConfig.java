package org.radarbase.authorizer.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceAuthorizationConfig {

    public enum DeviceType {FITBIT}

    @JsonProperty("device_type")
    private DeviceType deviceType;

    @JsonProperty("authorization_endpoint")
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("grant_type")
    private String grantType;

    @JsonProperty
    private String scope;

    @JsonProperty("client_id")
    private String clientId;

    @JsonProperty("client_secret")
    private String clientSecret;

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public DeviceAuthorizationConfig deviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public DeviceAuthorizationConfig authorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public DeviceAuthorizationConfig tokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    public String getGrantType() {
        return grantType;
    }

    public DeviceAuthorizationConfig grantType(String grantType) {
        this.grantType = grantType;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public DeviceAuthorizationConfig scope(String scope) {
        this.scope = scope;
        return this;
    }


    public String getClientId() {
        return clientId;
    }

    public DeviceAuthorizationConfig clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public DeviceAuthorizationConfig clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }

}
