/*
 *  Copyright 2026 The Hyve
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

package org.radarbase.authorizer.service

import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
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
import org.radarbase.authorizer.api.RestGoogleHealthIdentity
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.FITBIT_AUTH
import org.radarbase.authorizer.util.PkceUtil
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpInternalServerException
import org.slf4j.LoggerFactory

/**
 * OAuth2 + PKCE authorization service for the Google Health API.
 *
 * Google's token endpoint issues short-lived (~1h) access tokens and long-lived refresh
 * tokens that expire after 6 months of inactivity.
 *
 * On successful authorization this service also removes any prior FitBit authorization
 * for the same RADAR participant, so the legacy connector stops polling them once the
 * user has re-consented under Google Health.
 */
class GoogleHealthAuthorizationService(
    @param:Context private val clientService: RestSourceClientService,
    @param:Context private val registrationRepository: RegistrationRepository,
    @param:Context private val userRepository: RestSourceUserRepository,
    @param:Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clientService, config) {

    override suspend fun requestAccessToken(
        payload: RequestTokenPayload,
        sourceType: String,
        token: String?,
    ): RestOauth2AccessToken {
        val authConfig = clientService.forSourceType(sourceType)
        val registration = token?.let { registrationRepository.get(it) }
            ?: throw HttpInternalServerException(
                "internal_server_error",
                "registration not found for provided state token",
            )
        val codeVerifier = registration.codeVerifier ?: throw HttpInternalServerException(
            "internal_server_error",
            "code verifier not found for provided state token",
        )

        val response = withContext(Dispatchers.IO) {
            httpClient.submitForm(
                url = authConfig.tokenEndpoint,
                formParameters = Parameters.build {
                    payload.code?.let { append("code", it) }
                    append("grant_type", "authorization_code")
                    append("client_id", checkNotNull(authConfig.clientId))
                    append("client_secret", checkNotNull(authConfig.clientSecret))
                    append("redirect_uri", config.service.callbackUrl.toString())
                    append("code_verifier", codeVerifier)
                },
            )
        }
        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException(
                "Failed to request access token (HTTP status code ${response.status}): ${response.bodyAsText()}",
            )
        }

        val accessToken = response.body<RestOauth2AccessToken>()
        val externalUserId = getExternalId(accessToken.accessToken)

        cascadeDeregisterFitbit(registration.user)

        return accessToken.copy(externalUserId = externalUserId)
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        val authConfig = clientService.forSourceType(user.sourceType)

        logger.info("Refreshing GoogleHealth access token for user {}", user.userId)
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
                token.copy(
                    refreshToken = token.refreshToken ?: user.refreshToken,
                    externalUserId = token.externalUserId ?: user.externalUserId,
                )
            }
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                logger.error(
                    "Failed to refresh GoogleHealth token (HTTP status {}): {}",
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
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return false
        }
        return revokeAtGoogle(accessToken, user.userId ?: "unknown")
    }

    override suspend fun revokeToken(
        externalId: String,
        sourceType: String,
        token: String,
    ): Boolean = revokeAtGoogle(token, externalId)

    private suspend fun revokeAtGoogle(
        accessToken: String,
        userIdentifier: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val response = httpClient.post(GOOGLE_REVOKE_ENDPOINT) {
            parameter("token", accessToken)
        }
        if (response.status.isSuccess()) {
            logger.info("Revoked GoogleHealth token for user {}", userIdentifier)
            true
        } else {
            logger.error(
                "Failed to revoke GoogleHealth token for user {} (HTTP status {}): {}",
                userIdentifier,
                response.status,
                response.bodyAsText(),
            )
            false
        }
    }

    override suspend fun getAuthorizationEndpointWithParams(
        sourceType: String,
        userId: Long,
        state: String,
    ): String {
        val authConfig = clientService.forSourceType(sourceType)
        val codeVerifier = registrationRepository.get(state)?.codeVerifier
            ?: throw HttpInternalServerException(
                "internal_server_error",
                "code verifier not found for provided state token",
            )

        return URLBuilder().run {
            takeFrom(authConfig.authorizationEndpoint)
            parameters.append("response_type", "code")
            parameters.append("client_id", authConfig.clientId ?: "")
            parameters.append("scope", authConfig.scope ?: "")
            parameters.append("code_challenge", PkceUtil.generateCodeChallenge(codeVerifier))
            parameters.append("code_challenge_method", PKCE_CODE_CHALLENGE_METHOD)
            parameters.append("access_type", "offline")
            parameters.append("prompt", "consent")
            parameters.append("state", state)
            parameters.append("redirect_uri", config.service.callbackUrl.toString())
            buildString()
        }
    }

    override suspend fun deregisterUser(user: RestSourceUser) {
        userRepository.delete(user)
    }

    private suspend fun getExternalId(accessToken: String): String = withContext(Dispatchers.IO) {
        val response = httpClient.get(GOOGLE_HEALTH_IDENTITY_ENDPOINT) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        when (response.status) {
            HttpStatusCode.OK -> response.body<RestGoogleHealthIdentity>().healthUserId

            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                throw HttpBadGatewayException(
                    "Unable to fetch GoogleHealth identity (HTTP status ${response.status}): ${response.bodyAsText()}",
                )

            else -> throw HttpBadGatewayException(
                "Cannot connect to ${response.request.url}: HTTP status ${response.status}",
            )
        }
    }

    private suspend fun cascadeDeregisterFitbit(newUser: RestSourceUser) {
        val userId = newUser.userId ?: return
        val projectId = newUser.projectId ?: return
        val existing = runCatching {
            userRepository.findByUserIdProjectIdSourceType(userId, projectId, FITBIT_AUTH)
        }.onFailure {
            logger.warn("Cascade: lookup for existing FitBit record failed for user {}", userId, it)
        }.getOrNull() ?: return

        logger.info(
            "Cascade: removing legacy FitBit authorization (projectId={}, userId={}, id={}) after Google Health consent",
            projectId,
            userId,
            existing.id,
        )
        runCatching { userRepository.delete(existing) }.onFailure {
            logger.warn("Cascade: failed to delete legacy FitBit record for user {}", userId, it)
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoogleHealthAuthorizationService::class.java)

        private const val PKCE_CODE_CHALLENGE_METHOD = "S256"
        private const val GOOGLE_HEALTH_IDENTITY_ENDPOINT = "https://health.googleapis.com/v4/users/me/identity"
        private const val GOOGLE_REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke"
    }
}
