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
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.parameters
import io.ktor.http.takeFrom
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.glassfish.jersey.server.BackgroundScheduler
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RegistrationState
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GARMIN_AUTH
import org.radarbase.authorizer.service.GarminSourceAuthorizationService.Companion.DEREGISTER_CHECK_PERIOD
import org.radarbase.authorizer.util.PkceUtil
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpInternalServerException
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.kotlin.coroutines.forkJoin
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GarminAuthorizationService(
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
        token: String?
    ): RestOauth2AccessToken {
        val authConfig = clientService.forSourceType(sourceType)
        val codeVerifier = token?.let {
            registrationRepository.get(it)?.codeVerifier ?: throw HttpInternalServerException(
                "internal_server_error",
                "code verifier not found for state with token $it"
            )
        } ?: throw HttpInternalServerException(
            "internal_server_error",
            "token is null when requesting access token for source type $sourceType"
        )

        val response = withContext(Dispatchers.IO) {
            httpClient.submitForm {
                url {
                    takeFrom(authConfig.tokenEndpoint)
                }
                parameters {
                    payload.code?.let { append("code", it) }
                    append("grant_type", "authorization_code")
                    append("client_id", checkNotNull(authConfig.clientId))
                    append("client_secret", checkNotNull(authConfig.clientSecret))
                    append("redirect_uri", config.service.callbackUrl.toString())
                    append("code_verifier", codeVerifier)

                }
            }
        }
        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException("Failed to request access token (HTTP status code ${response.status}): ${response.bodyAsText()}")
        }
        response.body<RestOauth2AccessToken>().let {
        }

    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        val authConfig = clientService.forSourceType(user.sourceType)

        logger.info("Requesting to refresh token")
        val response = submitForm(user.sourceType) {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
            append("client_id", authConfig.clientId ?: "")
            append("client_secret", authConfig.clientSecret ?: "")
        }
        when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                logger.error("Failed to refresh token (HTTP status {}): {}", response.status, response.bodyAsText())
                null
            }

            else -> throw HttpBadGatewayException(
                "Cannot connect to ${response.request.url} (HTTP status ${response.status}): ${response.bodyAsText()}",
            )
        }
    }

    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        return super.revokeToken(user)
    }

    override suspend fun revokeToken(
        externalId: String,
        sourceType: String,
        token: String
    ): Boolean {
        return super.revokeToken(externalId, sourceType, token)
    }

    override suspend fun getAuthorizationEndpointWithParams(
        sourceType: String,
        userId: Long,
        state: String
    ): String {
        val authConfig = clientService.forSourceType(sourceType)
        val coderVerifier = registrationRepository.get(state)?.codeVerifier
            ?: throw HttpInternalServerException(
                "internal_server_error",
                "code verifier not found for state with token $state"
            )

        return URLBuilder().run {
            takeFrom(authConfig.authorizationEndpoint)
            parameters.append("response_type", "code")
            parameters.append("client_id", authConfig.clientId ?: "")
            parameters.append("code_challenge", pkceCodeChallenge(coderVerifier))
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

    override fun signRequest(
        user: RestSourceUser,
        payload: SignRequestParams
    ): SignRequestParams {
        return super.signRequest(user, payload)
    }

    companion object {
        private const val PKCE_CODE_CHALLENGE_METHOD = "S256"
        private const val GARMIN_USER_ID_ENDPOINT = "https://healthapi.garmin.com/wellness-api/rest/user/id"
        private const val DEREGISTER_CHECK_PERIOD = 3600000L

        private fun pkceCodeChallenge(codeVerifier: String) = PkceUtil.generateCodeChallenge(codeVerifier)
    }
}
