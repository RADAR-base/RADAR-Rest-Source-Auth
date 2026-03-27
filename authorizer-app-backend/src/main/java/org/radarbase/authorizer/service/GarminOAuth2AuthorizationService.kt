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

package org.radarbase.authorizer.service

import io.ktor.client.call.body
import io.ktor.client.request.delete
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
import org.glassfish.jersey.server.BackgroundScheduler
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestOauth2UserId
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GARMIN_AUTH
import org.radarbase.authorizer.util.PkceUtil
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpInternalServerException
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.kotlin.coroutines.forkJoin
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GarminOAuth2AuthorizationService(
    @param:Context private val clientService: RestSourceClientService,
    @param:Context private val registrationRepository: RegistrationRepository,
    @param:Context private val userRepository: RestSourceUserRepository,
    @param:Context private val asyncService: AsyncCoroutineService,
    @param:Context private val config: AuthorizerConfig,
    @param:Context
    @param:BackgroundScheduler
    private val scheduler: ScheduledExecutorService,
) : OAuth2RestSourceAuthorizationService(clientService, config) {

    init {
        scheduler.scheduleAtFixedRate(
            ::checkForUsersWithElapsedEndDateAndDeregister,
            0,
            DEREGISTER_CHECK_PERIOD,
            TimeUnit.MILLISECONDS,
        )
    }

    override suspend fun requestAccessToken(
        payload: RequestTokenPayload,
        sourceType: String,
        token: String?,
    ): RestOauth2AccessToken {
        val authConfig = clientService.forSourceType(sourceType)
        val codeVerifier = token?.let {
            registrationRepository.get(it)?.codeVerifier ?: throw HttpInternalServerException(
                "internal_server_error",
                "code verifier not found for state with token $it",
            )
        } ?: throw HttpInternalServerException(
            "internal_server_error",
            "token is null when requesting access token for source type $sourceType",
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
            throw HttpBadGatewayException("Failed to request access token (HTTP status code ${response.status}): ${response.bodyAsText()}")
        }

        return response.body<RestOauth2AccessToken>().run {
            this.copy(
                externalUserId = getExternalId(accessToken),
            )
        }
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        val authConfig = clientService.forSourceType(user.sourceType)

        logger.info("Requesting to refresh token")
        val response = httpClient.submitForm(
            url = authConfig.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", authConfig.clientId ?: "")
                append("client_secret", authConfig.clientSecret ?: "")
            },
        )

        when (response.status) {
            HttpStatusCode.OK -> {
                val token: RestOauth2AccessToken = response.body()
                token.copy(externalUserId = token.externalUserId ?: user.externalUserId)
            }
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                logger.error("Failed to refresh token (HTTP status {}): {}", response.status, response.bodyAsText())
                null
            }

            else -> throw HttpBadGatewayException(
                "Cannot connect to ${response.request.url} (HTTP status ${response.status}): ${response.bodyAsText()}",
            )
        }
    }

    /**
     * Revokes the user's Garmin OAuth2 token by sending a DELETE request
     * with Bearer authentication to the Garmin deregistration endpoint.
     * This overrides the parent's generic OAuth2 revocation which uses
     * token POST, as Garmin requires DELETE with Bearer auth instead.
     */
    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return false
        }
        return deleteRegistration(user.sourceType, accessToken, user.userId ?: "unknown")
    }

    override suspend fun revokeToken(
        externalId: String,
        sourceType: String,
        token: String,
    ): Boolean = deleteRegistration(sourceType, token, externalId)

    /**
     * Sends a DELETE request to the Garmin deregistration endpoint with Bearer auth
     * to revoke the given access token. [userIdentifier] is used only for logging.
     */
    private suspend fun deleteRegistration(
        sourceType: String,
        accessToken: String,
        userIdentifier: String,
    ): Boolean = withContext(Dispatchers.IO) {
        val authConfig = clientService.forSourceType(sourceType)
        val deregistrationEndpoint = checkNotNull(authConfig.deregistrationEndpoint) {
            "Missing Garmin deregistration endpoint configuration"
        }

        val response = httpClient.delete(deregistrationEndpoint) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.NoContent -> {
                logger.info("Successfully deregistered user {}", userIdentifier)
                true
            }
            else -> {
                logger.error(
                    "Failed to deregister user {} (HTTP status {}): {}",
                    userIdentifier,
                    response.status,
                    response.bodyAsText(),
                )
                false
            }
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
                "code verifier not found for state with token $state",
            )

        return URLBuilder().run {
            takeFrom(authConfig.authorizationEndpoint)
            parameters.append("response_type", "code")
            parameters.append("client_id", authConfig.clientId ?: "")
            parameters.append("code_challenge", pkceCodeChallenge(codeVerifier))
            parameters.append("code_challenge_method", PKCE_CODE_CHALLENGE_METHOD)
            parameters.append("state", state)
            parameters.append("redirect_uri", config.service.callbackUrl.toString())
            buildString()
        }
    }

    private fun checkForUsersWithElapsedEndDateAndDeregister() {
        asyncService.runBlocking {
            userRepository
                .queryAllWithElapsedEndDate(GARMIN_AUTH)
                .forkJoin { revokeToken(it) }
        }
    }

    override suspend fun deregisterUser(user: RestSourceUser) {
        userRepository.delete(user)
    }

    suspend fun getExternalId(accessToken: String): String = withContext(Dispatchers.IO) {
        val response = httpClient.get(GARMIN_USER_ID_ENDPOINT) {
            headers {
                append(HttpHeaders.Authorization, "Bearer $accessToken")
            }
        }

        when (response.status) {
            HttpStatusCode.OK -> response.body<RestOauth2UserId>().userId

            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw HttpBadGatewayException(
                "Service was unable to fetch the external ID",
            )

            else -> throw HttpBadGatewayException("Cannot connect to ${response.request.url}: HTTP status ${response.status}")
        }
    }

    companion object {
        private const val PKCE_CODE_CHALLENGE_METHOD = "S256"
        private const val GARMIN_USER_ID_ENDPOINT = "https://apis.garmin.com/wellness-api/rest/user/id"
        private const val DEREGISTER_CHECK_PERIOD = 3600000L

        private fun pkceCodeChallenge(codeVerifier: String) = PkceUtil.generateCodeChallenge(codeVerifier)
    }
}
