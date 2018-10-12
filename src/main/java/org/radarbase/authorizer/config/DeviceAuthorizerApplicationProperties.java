package org.radarbase.authorizer.config;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "device-clients", ignoreUnknownFields = false)
public class DeviceAuthorizerApplicationProperties {

    private List<DeviceAuthorizationConfig> deviceAuthConfigs;


    public List<DeviceAuthorizationConfig> getDeviceAuthConfigs() {
        return deviceAuthConfigs;
    }


    public void setDeviceAuthConfigs(List<DeviceAuthorizationConfig> deviceAuthConfigs) {
        this.deviceAuthConfigs = deviceAuthConfigs;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceAuthorizerApplicationProperties config = (DeviceAuthorizerApplicationProperties) o;
        return Objects.equals(deviceAuthConfigs, config.deviceAuthConfigs);
    }

    @Override
    public int hashCode() {

        return Objects.hash(deviceAuthConfigs);
    }
}
