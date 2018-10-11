package org.radarbase.authorizer.domain;


import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class DeviceClientDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String deviceType;

    private String authorizationEndpoint;

    private String tokenEndpoint;

    private String grantType;

    private String scope;

    private String clientId;

    private String clientSecret;

    public String getDeviceType() {
        return deviceType;
    }

    public DeviceClientDetails deviceType(String deviceType) {
        this.deviceType = deviceType;
        return this;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public DeviceClientDetails authorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
        return this;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public DeviceClientDetails tokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
        return this;
    }

    public String getGrantType() {
        return grantType;
    }

    public DeviceClientDetails grantType(String grantType) {
        this.grantType = grantType;
        return this;
    }

    public String getScope() {
        return scope;
    }

    public DeviceClientDetails scope(String scope) {
        this.scope = scope;
        return this;
    }


    public String getClientId() {
        return clientId;
    }

    public DeviceClientDetails clientId(String clientId) {
        this.clientId = clientId;
        return this;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public DeviceClientDetails clientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
        return this;
    }
}
