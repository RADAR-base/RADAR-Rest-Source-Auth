package org.radarbase.authorizer.api

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import java.io.Serializable
import java.time.Instant


@JsonIgnoreProperties(ignoreUnknown = true)
data class Oauth2AccessToken(
        @JsonProperty("access_token") var accessToken: String? = null,
        @JsonProperty("refresh_token") var refreshToken: String? = null,
        @JsonProperty("expires_in") var expiresIn: Int? = null,
        @JsonProperty("token_type") var tokenType: String? = null,
        @JsonProperty("user_id") var externalUserId: String? = null)


data class RestSourceClientDetailsDTO(
        var sourceType: String,
        var authorizationEndpoint: String,
        var tokenEndpoint: String,
        var grantType: String,
        var clientId: String,
        var scope: String? = null
)

data class RestSourceClients(
        var sourceClients: List<RestSourceClientDetailsDTO>
)

class RestSourceUserDTO(
        var id: String? = null,
        var projectId: String,
        var userId: String,
        var sourceId: String? = null,
        var externalUserId: String? = null,
        var startDate: Instant,
        var endDate: Instant? = null,
        var sourceType: String? = null,
        var isAuthorized: Boolean = false,
        var version: String? = null,
        var timesReset: Long = 0) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}

data class RestSourceUsers(
        var users: List<RestSourceUserDTO>
)

class TokenDTO(
        var accessToken: String? = null,
        var expiresAt: Instant? = null
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
