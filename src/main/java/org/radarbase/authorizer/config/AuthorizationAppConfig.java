package org.radarbase.authorizer.config;

import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AuthorizationAppConfig {

    @JsonProperty("device_auth_configs")
    private List<DeviceAuthorizationConfig> deviceAuthConfigs;

    @JsonProperty("config_files_location")
    private String configsFileLocation;

    public List<DeviceAuthorizationConfig> getDeviceAuthConfigs() {
        return deviceAuthConfigs;
    }

    public String getConfigsFileLocation() {
        return configsFileLocation;
    }

    @Override
    public String toString() {
        return "AuthorizationAppConfig{" + "deviceAuthConfigs=" + deviceAuthConfigs
                + ", configsFileLocation='" + configsFileLocation + '\'' + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AuthorizationAppConfig config = (AuthorizationAppConfig) o;
        return Objects.equals(deviceAuthConfigs, config.deviceAuthConfigs) && Objects
                .equals(configsFileLocation, config.configsFileLocation);
    }

    @Override
    public int hashCode() {

        return Objects.hash(deviceAuthConfigs, configsFileLocation);
    }
}
