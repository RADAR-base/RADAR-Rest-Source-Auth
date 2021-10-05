/*
 *  Copyright 2020 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.authorizer.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.time.Instant

@JsonIgnoreProperties(ignoreUnknown = true)
data class RestOauth2AccessToken(
    @JsonProperty("access_token") var accessToken: String,
    @JsonProperty("refresh_token") var refreshToken: String? = null,
    @JsonProperty("expires_in") var expiresIn: Int = 0,
    @JsonProperty("token_type") var tokenType: String? = null,
    @JsonProperty("user_id") var externalUserId: String? = null,
)

data class RestOauth1AccessToken(
    @JsonProperty("oauth_token") var token: String,
    @JsonProperty("oauth_token_secret") var tokenSecret: String? = null,
    @JsonProperty("oauth_verifier") var tokenVerifier: String? = null,
)

data class RestOauth1UserId(
    @JsonProperty("userId") var userId: String,
)

data class SignRequestParams(
    var url: String,
    var method: String,
    val parameters: Map<String, String?>,
)

data class StateCreateDTO(
    val userId: String,
    val persistent: Boolean = false,
)

data class TokenSecret(
    val secret: String,
)

data class RegistrationResponse(
    val token: String,
    val secret: String? = null,
    val userId: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val persistent: Boolean,
    val project: Project? = null,
    val authEndpointUrl: String? = null,
)

data class DeregistrationsDTO(
    val deregistrations: List<DeregistrationParams>
)

data class DeregistrationParams(
    val userId: String,
    val userAccessToken: String
)

data class RequestTokenPayload(
    var code: String? = null,
    var oauth_token: String? = null,
    var oauth_verifier: String? = null,
    var oauth_token_secret: String? = null,
)

data class ShareableClientDetail(
    val sourceType: String,
    val preAuthorizationEndpoint: String?,
    val deregistrationEndpoint: String?,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val grantType: String?,
    val clientId: String,
    val scope: String?,
)

data class ShareableClientDetails(
    val sourceClients: List<ShareableClientDetail>,
)

data class RestSourceUserDTO(
    val id: String?,
    val createdAt: Instant?,
    val projectId: String?,
    val userId: String?,
    val humanReadableUserId: String?,
    val externalId: String?,
    val sourceId: String?,
    val serviceUserId: String?,
    val startDate: Instant,
    val endDate: Instant? = null,
    val sourceType: String,
    var isAuthorized: Boolean = false,
    var registrationCreatedAt: Instant? = null,
    val hasValidToken: Boolean = false,
    val version: String? = null,
    val timesReset: Long = 0,
)

data class RestSourceUsers(
    val users: List<RestSourceUserDTO>,
    val metadata: Page?
)

data class TokenDTO(
    val accessToken: String?,
    val expiresAt: Instant?,
)

data class Page(
    val pageNumber: Int = 1,
    val pageSize: Int = Integer.MAX_VALUE,
    val totalElements: Long? = null
) {
    val offset: Int
        get() = (this.pageNumber - 1) * this.pageSize

    fun createValid(maximum: Int? = null): Page {
        val imposedNumber = pageNumber.coerceAtLeast(1)

        val imposedSize = if (maximum != null) {
            require(maximum >= 1) { "Maximum page size should be at least 1" }
            pageSize.coerceIn(1, maximum)
        } else {
            pageSize.coerceAtLeast(1)
        }
        return if (imposedNumber == pageNumber && imposedSize == pageSize) {
            this
        } else {
            copy(pageNumber = imposedNumber, pageSize = imposedSize)
        }
    }
}
