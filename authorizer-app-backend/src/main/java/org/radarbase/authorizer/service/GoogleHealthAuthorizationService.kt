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
    @param:Context private val authServices: RestSourceAuthorizationService,
    @param:Context private val subscriptionService: RestSourceUserSubscriptionService,
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
        val identity = fetchIdentity(accessToken.accessToken)

        cascadeDeregisterFitbit(registration.user, identity.legacyUserId)

        return accessToken.copy(externalUserId = identity.healthUserId)
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
                // The user can no longer be served; remove their subscription so PINGs stop.
                subscriptionService.unsubscribe(user)
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
        val revoked = revokeAtGoogle(accessToken, user.userId ?: "unknown")
        // Revoking the token ends data flow, so the subscription is no longer useful.
        subscriptionService.unsubscribe(user)
        return revoked
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
        // Remove the webhook subscription before the user row (and its subscription row) are deleted.
        subscriptionService.removeForUser(user)
        userRepository.delete(user)
    }

    private suspend fun fetchIdentity(accessToken: String): RestGoogleHealthIdentity = withContext(Dispatchers.IO) {
        val response = httpClient.get(GOOGLE_HEALTH_IDENTITY_ENDPOINT) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        when (response.status) {
            HttpStatusCode.OK -> response.body<RestGoogleHealthIdentity>()

            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden ->
                throw HttpBadGatewayException(
                    "Unable to fetch GoogleHealth identity (HTTP status ${response.status}): ${response.bodyAsText()}",
                )

            else -> throw HttpBadGatewayException(
                "Cannot connect to ${response.request.url}: HTTP status ${response.status}",
            )
        }
    }

    private suspend fun cascadeDeregisterFitbit(newUser: RestSourceUser, legacyUserId: String?) {
        val cleanedIds = mutableSetOf<Long>()

        val radarUserId = newUser.userId
        val projectId = newUser.projectId
        if (radarUserId != null && projectId != null) {
            runCatching {
                userRepository.findByUserIdProjectIdSourceType(radarUserId, projectId, FITBIT_AUTH)
            }.onFailure {
                logger.warn("Cascade: lookup by RADAR identity failed for user {}", radarUserId, it)
            }.getOrNull()?.let { fitbitUser ->
                revokeAndDelete(fitbitUser, "RADAR identity (projectId=$projectId, userId=$radarUserId)")
                fitbitUser.id?.let(cleanedIds::add)
            }
        }

        if (legacyUserId != null) {
            runCatching {
                userRepository.findByExternalId(legacyUserId, FITBIT_AUTH)
            }.onFailure {
                logger.warn("Cascade: lookup by externalUserId={} failed", legacyUserId, it)
            }.getOrNull()
                ?.takeIf { it.id !in cleanedIds }
                ?.let { fitbitUser ->
                    revokeAndDelete(fitbitUser, "Google legacyUserId=$legacyUserId")
                }
        }
    }

    private suspend fun revokeAndDelete(user: RestSourceUser, matchedBy: String) {
        val id = user.id
        logger.info(
            "Cascade: removing legacy FitBit authorization id={} (matched by {}, projectId={}, userId={}, externalUserId={})",
            id,
            matchedBy,
            user.projectId,
            user.userId,
            user.externalUserId,
        )
        runCatching { authServices.revokeToken(user) }
            .onSuccess { ok ->
                if (ok) {
                    logger.info("Cascade: revoked Fitbit token for id={}", id)
                } else {
                    logger.info("Cascade: Fitbit revoke returned false for id={} (token may already be invalid)", id)
                }
            }
            .onFailure { logger.warn("Cascade: Fitbit revoke threw for id={} — proceeding with delete", id, it) }

        runCatching { userRepository.delete(user) }
            .onSuccess { logger.info("Cascade: deleted FitBit row id={}", id) }
            .onFailure { logger.warn("Cascade: failed to delete FitBit row id={}", id, it) }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(GoogleHealthAuthorizationService::class.java)

        private const val PKCE_CODE_CHALLENGE_METHOD = "S256"
        private const val GOOGLE_HEALTH_IDENTITY_ENDPOINT = "https://health.googleapis.com/v4/users/me/identity"
        private const val GOOGLE_REVOKE_ENDPOINT = "https://oauth2.googleapis.com/revoke"
    }
}
