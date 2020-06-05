/*
 *
 *  * Copyright 2018 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer.config;

import java.util.Objects;

public class RestSourceClientConfig {

    private String sourceType;

    private String authorizationEndpoint;

    private String tokenEndpoint;

    private String grantType;

    private String scope;

    private String clientId;

    private String clientSecret;

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getGrantType() {
        return grantType;
    }

    public void setGrantType(String grantType) {
        this.grantType = grantType;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        RestSourceClientConfig that = (RestSourceClientConfig) o;
        return Objects.equals(sourceType, that.sourceType) && Objects
                .equals(authorizationEndpoint, that.authorizationEndpoint) && Objects
                .equals(tokenEndpoint, that.tokenEndpoint) && Objects
                .equals(grantType, that.grantType) && Objects.equals(scope, that.scope) && Objects
                .equals(clientId, that.clientId) && Objects.equals(clientSecret, that.clientSecret);
    }

    @Override
    public int hashCode() {

        return Objects
                .hash(sourceType, authorizationEndpoint, tokenEndpoint, grantType, scope, clientId,
                        clientSecret);
    }
}
