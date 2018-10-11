package org.radarbase.authorizer.config;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "device-clients", ignoreUnknownFields = false)
public class DeviceAuthorizerApplicationProperties {

    private List<DeviceAuthorizationConfig> deviceAuthConfigs;

    private String configsFileLocation;

    public List<DeviceAuthorizationConfig> getDeviceAuthConfigs() {
        return deviceAuthConfigs;
    }

    public String getConfigsFileLocation() {
        return configsFileLocation;
    }

    public void setDeviceAuthConfigs(List<DeviceAuthorizationConfig> deviceAuthConfigs) {
        this.deviceAuthConfigs = deviceAuthConfigs;
    }

    public void setConfigsFileLocation(String configsFileLocation) {
        this.configsFileLocation = configsFileLocation;
    }

    @Override
    public String toString() {
        return "DeviceAuthorizerApplicationProperties{" + "deviceAuthConfigs=" + deviceAuthConfigs
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
        DeviceAuthorizerApplicationProperties config = (DeviceAuthorizerApplicationProperties) o;
        return Objects.equals(deviceAuthConfigs, config.deviceAuthConfigs) && Objects
                .equals(configsFileLocation, config.configsFileLocation);
    }

    @Override
    public int hashCode() {

        return Objects.hash(deviceAuthConfigs, configsFileLocation);
    }
}
