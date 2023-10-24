package org.radarbase.authorizer.service

import io.ktor.client.request.basicAuth
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RestSourceUser

class OuraAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, config) {

    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return false
        }
        // revoke token using the deregistrationEndpoint token endpoint
        val authConfig = clients.forSourceType(user.sourceType)
        val deregistrationEndpoint = checkNotNull(authConfig.deregistrationEndpoint)

        val isSuccess = try {
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
                logger.error("Failed to revoke token for user {}: {}", user.userId, response.bodyAsText().take(512))
                false
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
}
