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
