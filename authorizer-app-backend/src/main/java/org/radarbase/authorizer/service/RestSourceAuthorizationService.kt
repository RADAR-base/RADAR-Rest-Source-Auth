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
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.util.request
import org.radarbase.authorizer.util.requestValue
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context

class RestSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper
) {
    private val tokenReader = objectMapper.readerFor(RestOauth2AccessToken::class.java)
    private val configMap = restSourceClients.clients.map { it.sourceType to it }.toMap()

    fun requestAccessToken(code: String, sourceType: String): RestOauth2AccessToken {
        val authorizationConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        val form = FormBody.Builder()
            .add("code", code)
            .add("grant_type", "authorization_code")
            .add("client_id", authorizationConfig.clientId)
            .build();
        logger.info("Requesting access token with authorization code")
        return httpClient.requestValue(post(form, sourceType), tokenReader)
    }

    fun refreshToken(refreshToken: String, sourceType: String): RestOauth2AccessToken {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build();
        logger.info("Requesting to refreshToken")
        return httpClient.requestValue(post(form, sourceType), tokenReader)
    }

    fun revokeToken(accessToken: String, sourceType: String): Boolean {
        val form = FormBody.Builder().add("token", accessToken).build();
        logger.info("Requesting to revoke access token");

        return httpClient.request(post(form, sourceType))
    }

    private fun post(form: FormBody, sourceType: String): Request {
        val authorizationConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        return Request.Builder().apply {
            url(authorizationConfig.tokenEndpoint)
            post(form)
            header("Authorization", Credentials.basic(authorizationConfig.clientId, authorizationConfig.clientSecret))
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Accept", "application/json")
        }.build()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RestSourceAuthorizationService::class.java)
    }
}
