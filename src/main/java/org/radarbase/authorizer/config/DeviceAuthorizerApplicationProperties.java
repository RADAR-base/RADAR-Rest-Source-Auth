package org.radarbase.authorizer.config;

import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

@ConfigurationProperties(prefix = "device-clients", ignoreUnknownFields = false)
public class DeviceAuthorizerApplicationProperties {

    private List<DeviceAuthorizationConfig> deviceAuthConfigs;

    private CorsConfiguration cors;

    public List<DeviceAuthorizationConfig> getDeviceAuthConfigs() {
        return deviceAuthConfigs;
    }


    public void setDeviceAuthConfigs(List<DeviceAuthorizationConfig> deviceAuthConfigs) {
        this.deviceAuthConfigs = deviceAuthConfigs;
    }


    public CorsConfiguration getCors() {
        return cors;
    }

    public void setCors(CorsConfiguration cors) {
        this.cors = cors;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        DeviceAuthorizerApplicationProperties that = (DeviceAuthorizerApplicationProperties) o;
        return Objects.equals(deviceAuthConfigs, that.deviceAuthConfigs) && Objects
                .equals(cors, that.cors);
    }

    @Override
    public int hashCode() {

        return Objects.hash(deviceAuthConfigs, cors);
    }
}
