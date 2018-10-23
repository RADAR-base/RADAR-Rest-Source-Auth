package org.radarbase.authorizer.config;

import java.util.List;


public class DeviceClients {

    private List<DeviceAuthorizationConfig> deviceClients;

    public List<DeviceAuthorizationConfig> getDeviceClients() {
        return deviceClients;
    }

    public void setDeviceClients(List<DeviceAuthorizationConfig> deviceClients) {
        this.deviceClients = deviceClients;
    }
}
