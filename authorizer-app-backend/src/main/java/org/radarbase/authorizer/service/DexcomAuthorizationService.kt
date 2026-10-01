package org.radarbase.authorizer.service

import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestOauth2UserId
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.slf4j.LoggerFactory

/**
 * Dexcom OAuth2 authorization service.
 *
 * Dexcom requires client_id and client_secret as form parameters in the token
 * request rather than HTTP Basic authentication. Dexcom does not expose a token
 * revocation endpoint; users revoke access via Dexcom account settings.
 */
class DexcomAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, config) {

    override suspend fun requestAccessToken(
        payload: RequestTokenPayload,
        sourceType: String,
    ): RestOauth2AccessToken = withContext(Dispatchers.IO) {
        logger.info("Requesting Dexcom access token with authorization code")
        val authConfig = clients.forSourceType(sourceType)
        val response = httpClient.submitForm(
            url = authConfig.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                payload.code?.let { append("code", it) }
                append("client_id", checkNotNull(authConfig.clientId))
                append("client_secret", checkNotNull(authConfig.clientSecret))
                append("redirect_uri", config.service.callbackUrl.toString())
            },
        )
        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException(
                "Failed to request access token (HTTP status code ${response.status}): ${response.bodyAsText()}",
            )
        }

        val accessToken = response.body<RestOauth2AccessToken>()
        accessToken.copy(
            externalUserId = accessToken.externalUserId ?: getExternalId(
                accessToken.accessToken,
                authConfig.tokenEndpoint,
            ),
        )
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        val authConfig = clients.forSourceType(user.sourceType)

        logger.info("Requesting to refresh Dexcom token")
        val response = httpClient.submitForm(
            url = authConfig.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", checkNotNull(authConfig.clientId))
                append("client_secret", checkNotNull(authConfig.clientSecret))
            },
        )

        when (response.status) {
            HttpStatusCode.OK -> {
                val token: RestOauth2AccessToken = response.body()
                token.copy(externalUserId = token.externalUserId ?: user.externalUserId)
            }
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                logger.error(
                    "Failed to refresh Dexcom token (HTTP status {}): {}",
                    response.status,
                    response.bodyAsText(),
                )
                null
            }
            else -> throw HttpBadGatewayException(
                "Cannot connect to ${response.request.url} (HTTP status ${response.status}): ${response.bodyAsText()}",
            )
        }
    }

    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        logger.error("Token revocation not supported for Dexcom")
        return false
    }

    private suspend fun getExternalId(accessToken: String, tokenEndpoint: String): String {
        val dataRangeUrl = URLBuilder().apply {
            takeFrom(tokenEndpoint)
            pathSegments = listOf("v3", "users", "self", "dataRange")
        }.buildString()

        val response = httpClient.get(dataRangeUrl) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException(
                "Unable to fetch Dexcom user ID from $dataRangeUrl (HTTP status ${response.status}): ${response.bodyAsText()}",
            )
        }

        return response.body<RestOauth2UserId>().userId
    }

    companion object {
        private val logger = LoggerFactory.getLogger(DexcomAuthorizationService::class.java)
    }
}
