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
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RegistrationState
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.PkceUtil
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpInternalServerException
import org.radarbase.jersey.service.AsyncCoroutineService

class GarminAuthorizationService(
    @param:Context private val clientService: RestSourceClientService,
    @param:Context private val registrationRepository: RegistrationRepository,
    @param:Context private val userRepository: RestSourceUserRepository,
    @param:Context private val asyncService: AsyncCoroutineService,
    @param:Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clientService, config) {

    override suspend fun requestAccessToken(
        payload: RequestTokenPayload,
        sourceType: String
    ): RestOauth2AccessToken {
        logger.info("Requesting access token with authorization code")
        val response = submitForm(sourceType) { authorizationConfig ->
            payload.code?.let { append("code", it) }
            append("grant_type", "authorization_code")
            append("client_id", checkNotNull(authorizationConfig.clientId))
            append("client_secret", checkNotNull(authorizationConfig.clientSecret))
            append("redirect_uri", config.service.callbackUrl.toString())
            append("code", checkNotNull(payload.code))
            append("code_verifier", "")
        }
        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException("Failed to request access token (HTTP status code ${response.status}): ${response.bodyAsText()}")
        }
        return response.body()
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        logger.info("Requesting to refresh token")
        val response = submitForm(user.sourceType) {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
            append("client_id", "")
            append("client_secret", "")
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
        val coderVerifier = registrationRepository.get(state)?.codeVerifier ?: throw HttpInternalServerException("internal_server_error", "code verifier not found for state $state")

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

    override suspend fun deregisterUser(user: RestSourceUser): Nothing {
        return super.deregisterUser(user)
    }

    override fun signRequest(
        user: RestSourceUser,
        payload: SignRequestParams
    ): SignRequestParams {
        return super.signRequest(user, payload)
    }

    companion object {
        private const val PKCE_CODE_CHALLENGE_METHOD = "S256"
        private fun pkceCodeChallenge(codeVerifier: String) = PkceUtil.generateCodeChallenge(codeVerifier)
    }
}
