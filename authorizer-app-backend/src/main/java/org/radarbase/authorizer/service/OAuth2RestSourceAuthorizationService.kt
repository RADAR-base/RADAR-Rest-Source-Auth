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

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriBuilder
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.radarbase.authorizer.Config
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.util.request
import org.radarbase.jersey.util.requestJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OAuth2RestSourceAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper,
    @Context private val config: Config,
) : RestSourceAuthorizationService {
    private val tokenReader = objectMapper.readerFor(RestOauth2AccessToken::class.java)

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val authorizationConfig = clients.forSourceType(sourceType)
        val clientId = checkNotNull(authorizationConfig.clientId)

        val form = FormBody.Builder().apply {
            payload.code?.let { add("code", it) }
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("redirect_uri", config.service.callbackUrl.toString())
        }.build()
        logger.info("Requesting access token with authorization code")
        return httpClient.requestJson(post(form, sourceType), tokenReader)
    }

    override fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? {
        val form = FormBody.Builder().apply {
            add("grant_type", "refresh_token")
            user.refreshToken?.let { add("refresh_token", it) }
        }.build()
        logger.info("Requesting to refreshToken")
        val request = post(form, user.sourceType)
        return httpClient.newCall(request).execute().use { response ->
            when (response.code) {
                200 -> response.body?.byteStream()
                    ?.let { tokenReader.readValue<RestOauth2AccessToken>(it) }
                    ?: throw HttpBadGatewayException("Service ${user.sourceType} did not provide a result")
                400, 401, 403 -> {
                    val body = response.body?.string()
                    logger.error("Failed to refresh token (HTTP status code {}): {}", response.code, body)
                    null
                }
                else -> throw HttpBadGatewayException("Cannot connect to ${request.url}: HTTP status ${response.code}")
            }
        }
    }

    override fun revokeToken(user: RestSourceUser): Boolean {
        val form = FormBody.Builder().add("token", user.accessToken!!).build()
        logger.info("Requesting to revoke access token")
        return httpClient.request(post(form, user.sourceType))
    }

    override fun revokeToken(externalId: String, sourceType: String, token: String): Boolean =
        throw HttpBadRequestException("", "Not available for auth type")

    override fun getAuthorizationEndpointWithParams(
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

    override fun deregisterUser(user: RestSourceUser) =
        throw HttpBadRequestException("", "Not available for auth type")

    override fun signRequest(user: RestSourceUser, payload: SignRequestParams): SignRequestParams =
        throw HttpBadRequestException("", "Not available for auth type")

    private fun post(form: FormBody, sourceType: String): Request {
        val authorizationConfig = clients.forSourceType(sourceType)

        val credentials = Credentials.basic(
            checkNotNull(authorizationConfig.clientId),
            checkNotNull(authorizationConfig.clientSecret),
        )

        return Request.Builder().apply {
            url(authorizationConfig.tokenEndpoint)
            post(form)
            header("Authorization", credentials)
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Accept", "application/json")
        }.build()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OAuth2RestSourceAuthorizationService::class.java)
    }
}
