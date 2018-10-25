package org.radarbase.authorizer.service.dto;

import java.time.Instant;

public class TokenDTO {

    private String accessToken;

    private Instant expiresAt;

    public String getAccessToken() {
        return accessToken;
    }

    public TokenDTO accessToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public TokenDTO expiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
        return this;
    }
}
