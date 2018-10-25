package org.radarbase.authorizer.service.dto;

import java.util.List;

public class DeviceUsersDTO {

    List<DeviceUserPropertiesDTO> users;

    public List<DeviceUserPropertiesDTO> getUsers() {
        return users;
    }

    public DeviceUsersDTO users(List<DeviceUserPropertiesDTO> users) {
        this.users = users;
        return this;
    }
}
