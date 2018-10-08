package org.radarbase.authorizer.service.dto;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DeviceAccessToken {

    @JsonProperty("user_id")
    private String externalUserId;


}
