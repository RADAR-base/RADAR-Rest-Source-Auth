package org.radarbase.authorizer.service.dto;

public class DeviceAuthorizationUrl {

    private String authorizationUrl;

    public String getAuthorizationUrl() {
        return authorizationUrl;
    }

    public DeviceAuthorizationUrl authorizationUrl(String authorizationUrl) {
        this.authorizationUrl = authorizationUrl;
        return this;
    }
}
