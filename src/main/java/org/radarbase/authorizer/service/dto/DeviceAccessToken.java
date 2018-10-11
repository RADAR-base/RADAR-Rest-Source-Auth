package org.radarbase.authorizer.service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceAccessToken extends Oauth2AccessToken{

    @JsonProperty("user_id")
    private String externalUserId;

    public String getExternalUserId() {
        return externalUserId;
    }

    public void setExternalUserId(String externalUserId) {
        this.externalUserId = externalUserId;
    }
}
