package org.radarbase.authorizer.service.dto;

import org.radarbase.authorizer.config.DeviceAuthorizationConfig;

public class DeviceClientDetailsDTO {

    private String deviceType;

    private String authorizationEndpoint;

    private String tokenEndpoint;

    private String grantType;

    private String scope;

    private String clientId;

    public DeviceClientDetailsDTO() {
    }

    public DeviceClientDetailsDTO(DeviceAuthorizationConfig deviceAuthorizationConfig) {
        this.authorizationEndpoint = deviceAuthorizationConfig.getAuthorizationEndpoint();
        this.deviceType = deviceAuthorizationConfig.getDeviceType();
        this.grantType = deviceAuthorizationConfig.getGrantType();
        this.clientId = deviceAuthorizationConfig.getClientId();
        this.scope = deviceAuthorizationConfig.getScope();
    }

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
}
