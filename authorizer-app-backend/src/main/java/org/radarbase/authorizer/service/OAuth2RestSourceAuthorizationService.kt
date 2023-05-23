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

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.config.RestSourceClient
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OAuth2RestSourceAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val config: AuthorizerConfig,
) : RestSourceAuthorizationService {
    private val httpClient = RestSourceAuthorizationService.httpClient()

    override suspend fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken = withContext(Dispatchers.IO) {
        logger.info("Requesting access token with authorization code")
        val response = submitForm(sourceType) { authorizationConfig ->
            payload.code?.let { append("code", it) }
            append("grant_type", "authorization_code")
            append("client_id", checkNotNull(authorizationConfig.clientId))
            append("redirect_uri", config.service.callbackUrl.toString())
        }
        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException("Failed to request access token (HTTP status code ${response.status}): ${response.bodyAsText()}")
        }
        response.body()
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        logger.info("Requesting to refresh token")
        val response = submitForm(user.sourceType) {
            append("grant_type", "refresh_token")
            append("refresh_token", refreshToken)
        }
        when (response.status) {
            HttpStatusCode.OK -> response.body()
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                logger.error("Failed to refresh token (HTTP status code {}): {}", response.status, response.bodyAsText())
                null
            }
            else -> throw HttpBadGatewayException(
                "Cannot connect to ${response.request.url} (HTTP status ${response.status}): ${response.bodyAsText()}"
            )
        }
    }

    override suspend fun revokeToken(user: RestSourceUser): Boolean = withContext(Dispatchers.IO) {
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return@withContext false
        }
        logger.info("Requesting to revoke access token")
        val response = submitForm(user.sourceType) {
            append("token", accessToken)
        }
        response.status.isSuccess()
    }

    override suspend fun revokeToken(externalId: String, sourceType: String, token: String): Boolean =
        throw HttpBadRequestException("", "Not available for auth type")

    override suspend fun getAuthorizationEndpointWithParams(
        sourceType: String,
        userId: Long,
        state: String,
    ): String {
        val authConfig = clients.forSourceType(sourceType)
        return UriBuilder.fromUri(authConfig.authorizationEndpoint)
            .queryParam("response_type", "code")
            .queryParam("client_id", authConfig.clientId)
            .queryParam("state", state)
            .queryParam("scope", authConfig.scope)
            .queryParam("prompt", "login")
            .queryParam("redirect_uri", config.service.callbackUrl)
            .build().toString()
    }

    override suspend fun deregisterUser(user: RestSourceUser) =
        throw HttpBadRequestException("", "Not available for auth type")

    override fun signRequest(user: RestSourceUser, payload: SignRequestParams): SignRequestParams =
        throw HttpBadRequestException("", "Not available for auth type")

    private suspend fun submitForm(sourceType: String, builder: ParametersBuilder.(RestSourceClient) -> Unit): HttpResponse {
        val authorizationConfig = clients.forSourceType(sourceType)

        return httpClient.submitForm(
            url = authorizationConfig.tokenEndpoint,
            formParameters = Parameters.build {
                builder(authorizationConfig)
            }
        ) {
            basicAuth(
                checkNotNull(authorizationConfig.clientId),
                checkNotNull(authorizationConfig.clientSecret),
            )
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OAuth2RestSourceAuthorizationService::class.java)
    }
}
