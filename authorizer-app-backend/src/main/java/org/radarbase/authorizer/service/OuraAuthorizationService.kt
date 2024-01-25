package org.radarbase.authorizer.service

import io.ktor.client.call.body
import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.api.OuraAuthUserId
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import java.io.IOException

class OuraAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, config) {
    override suspend fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val accessToken: RestOauth2AccessToken = super.requestAccessToken(payload, sourceType)
        return accessToken.copy(externalUserId = getExternalId(accessToken.accessToken))
    }

    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return false
        }
        // revoke token using the deregistrationEndpoint token endpoint
        val authConfig = clients.forSourceType(user.sourceType)
        val deregistrationEndpoint = checkNotNull(authConfig.deregistrationEndpoint) { "Missing Oura deregistration endpoint configuration" }

        val isSuccess = try {
            withContext(Dispatchers.IO) {
                val response = httpClient.submitForm {
                    url {
                        takeFrom(deregistrationEndpoint)
                        parameters.append("access_token", accessToken)
                    }
                    basicAuth(
                        username = checkNotNull(authConfig.clientId),
                        password = checkNotNull(authConfig.clientSecret),
                    )
                }
                if (response.status.isSuccess()) {
                    true
                } else {
                    logger.error(
                        "Failed to revoke token for user {}: {}",
                        user.userId,
                        response.bodyAsText().take(512),
                    )
                    false
                }
            }
        } catch (ex: Exception) {
            logger.warn("Revoke endpoint error: {}", ex.toString())
            false
        }

        return if (isSuccess) {
            logger.info("Successfully revoked token for user {}", user.userId)
            true
        } else {
            logger.error("Failed to revoke token for user {}", user.userId)
            false
        }
    }

    private suspend fun getExternalId(accessToken: String): String = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get {
                url(OURA_USER_ID_ENDPOINT)
                headers {
                    append(HttpHeaders.Authorization, "Bearer ${accessToken}")
                }
            }
            if (response.status.isSuccess()) {
                response.body<OuraAuthUserId>().userId
            } else {
                logger.error(
                    "Unable to fetch data from Oura $OURA_USER_ID_ENDPOINT (Http Status {}): {}",
                    response.status,
                    response.bodyAsText().take(512),
                )
                throw HttpBadGatewayException("Cannot connect to $OURA_USER_ID_ENDPOINT: HTTP status ${response.status}")
            }
        } catch (ex: IOException) {
            logger.error("Unable to fetch data from Oura $OURA_USER_ID_ENDPOINT: {}", ex.toString())
            throw HttpBadGatewayException("Cannot connect to $OURA_USER_ID_ENDPOINT: I/O error")
        }
    }

    companion object {
        private const val OURA_USER_ID_ENDPOINT = "https://api.ouraring.com/v2/usercollection/personal_info"
    }
}
