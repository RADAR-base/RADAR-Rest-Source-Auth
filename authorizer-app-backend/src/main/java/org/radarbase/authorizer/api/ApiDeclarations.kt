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
import java.io.Serializable
import java.time.Instant


@JsonIgnoreProperties(ignoreUnknown = true)
data class RestOauth2AccessToken(
    @JsonProperty("access_token") var accessToken: String,
    @JsonProperty("refresh_token") var refreshToken: String? = null,
    @JsonProperty("expires_in") var expiresIn: Int = 0,
    @JsonProperty("token_type") var tokenType: String? = null,
    @JsonProperty("user_id") var externalUserId: String? = null)

data class RestOauth1AccessToken(
    @JsonProperty("oauth_token") var token: String,
    @JsonProperty("oauth_token_secret") var tokenSecret: String? = null,
    @JsonProperty("oauth_verifier") var tokenVerifier: String? = null
)

data class RequestTokenPayload(
        var code: String? = null,
        var requestToken: String? = null,
        var requestTokenVerifier: String? = null,
        var state: String?= null
)

data class ShareableClientDetail(
    val sourceType: String,
    val preAuthorizationEndpoint: String?,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val grantType: String?,
    val clientId: String,
    val scope: String?,
    val state: String? = null
)

data class ShareableClientDetails(
    val sourceClients: List<ShareableClientDetail>
)

class RestSourceUserDTO(
    val id: String?,
    val projectId: String?,
    val userId: String?,
    val sourceId: String,
    val externalUserId: String,
    val startDate: Instant,
    val endDate: Instant? = null,
    val sourceType: String,
    val isAuthorized: Boolean = false,
    val hasValidToken: Boolean = false,
    val version: String? = null,
    val timesReset: Long = 0) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class RestSourceUsers(
    val users: List<RestSourceUserDTO>
)

class TokenDTO(
    val accessToken: String?,
    val expiresAt: Instant?
)

data class Page(
    val pageNumber: Int = 1,
    val pageSize: Int? = null,
    val totalElements: Long? = null) {
    val offset: Int
        get() = (this.pageNumber - 1) * this.pageSize!!

    fun createValid(maximum: Int? = null): Page {
        val imposedNumber = pageNumber.coerceAtLeast(1)

        val imposedSize = if (maximum != null) {
            require(maximum >= 1) { "Maximum page size should be at least 1" }
            pageSize?.coerceAtLeast(1)?.coerceAtMost(maximum) ?: maximum
        } else {
            pageSize?.coerceAtLeast(1)
        }
        return if (imposedNumber == pageNumber && imposedSize == pageSize) {
            this
        } else {
            copy(pageNumber = imposedNumber, pageSize = imposedSize)
        }
    }
}
