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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import org.radarbase.authorizer.RestSourceClient
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth1AccessToken
import org.radarbase.authorizer.api.RestOauth1UserId
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.OauthSignature
import org.radarbase.authorizer.util.Url
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import java.util.*
import javax.ws.rs.core.Context

open class OAuth1RestSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper
): RestSourceAuthorizationService(restSourceClients, httpClient, objectMapper) {
    private val configMap = restSourceClients.clients.map { it.sourceType to it }.toMap()
    private val tokenReader = objectMapper.readerFor(RestOauth1AccessToken::class.java)

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken? {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")
        logger.info("Requesting access token..")

        val tokens = this.requestToken(authConfig.tokenEndpoint, RestOauth1AccessToken(payload.oauth_token!!, payload.oauth_token_secret, payload.oauth_verifier), sourceType)
        return tokens?.let { mapToOauth2(it, sourceType) }
    }

    override fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? {
        return user.accessToken?.let { RestOauth2AccessToken(it, user.refreshToken) }
    }

    override fun revokeToken(user: RestSourceUser): Boolean {
        val authConfig = configMap[user.sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type ${user.sourceType}")
        val req = createRequest("DELETE", authConfig.deRegistrationEndpoint!!, RestOauth1AccessToken(user.accessToken!!, user.refreshToken), user.sourceType)

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200, 204 -> true
                400, 401, 403 -> false
                else -> throw HttpBadGatewayException("Cannot connect to ${authConfig.deRegistrationEndpoint}: HTTP status ${response.code}")
            }
        }
    }

    override fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String {
        logger.info("Getting auth endpoint..")
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        val tokens = this.requestToken(authConfig.preAuthorizationEndpoint, RestOauth1AccessToken(""), sourceType)
        val params = mutableMapOf<String, String?>()
        params[OAUTH_ACCESS_TOKEN] = tokens?.token
        params[OAUTH_ACCESS_TOKEN_SECRET] = tokens?.tokenSecret
        params[OAUTH_CALLBACK] = callBackUrl

        return Url(authConfig.authorizationEndpoint, params).getUrl()
    }

    fun requestToken(tokenEndpoint: String?, tokens: RestOauth1AccessToken, sourceType: String): RestOauth1AccessToken? {
        val req = createRequest("POST", tokenEndpoint.orEmpty(), tokens, sourceType)

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200 -> response.body?.string()
                        ?.let { tokenReader.readValue<RestOauth1AccessToken>(parseParams(it)) }
                        ?: throw HttpBadGatewayException("Service did not provide a result")
                400, 401, 403 ->{
                    logger.info(response.body?.string())
                    null }
                else -> throw HttpBadGatewayException("Cannot connect to ${tokenEndpoint}: HTTP status ${response.code}")
            }
        }
    }

    fun createRequest(method: String, url: String, tokens: RestOauth1AccessToken, sourceType: String): Request {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type ${sourceType}")
        var params = this.getCommonAuthParams(authConfig, tokens.token, tokens.tokenVerifier)
        val signature = OauthSignature(url, params, method, authConfig.clientSecret, tokens.tokenSecret).getEncodedSignature()
        params[OAUTH_SIGNATURE] = signature
        val headers = mapToList(params)

        val req: Request = Request.Builder()
                .url(url)
                .header("Authorization", "OAuth $headers")
                .method(method, if (method == "POST") RequestBody.create(null, "") else null)
                .build()
        return req

    }

    private fun getCommonAuthParams(authConfig: RestSourceClient, accessToken: String?, tokenVerifier: String?): MutableMap<String, String?> {
        val params = mutableMapOf<String, String?>()
        params[OAUTH_CONSUMER_KEY] = authConfig.clientId
        params[OAUTH_NONCE] = this.generateNonce()
        params[OAUTH_SIGNATURE_METHOD] = OAUTH_SIGNATURE_METHOD_VALUE
        params[OAUTH_TIMESTAMP] = Instant.now().epochSecond.toString()
        params[OAUTH_ACCESS_TOKEN] = accessToken
        params[OAUTH_VERIFIER] = tokenVerifier
        params[OAUTH_VERSION] = OAUTH_VERSION_VALUE
        return params
    }

    open fun getExternalId(tokens: RestOauth1AccessToken, sourceType: String): String? {
        return UUID.randomUUID().toString()
    }

    fun generateNonce(): String {
        return Math.floor(Math.random() * 1000000000).toInt().toString();
    }

    fun parseParams(input: String): String? {
        var params = input
        params = params.replace("=".toRegex(), "\":\"")
        params = params.replace("&".toRegex(), "\",\"")
        return "{\"$params\"}"
    }

    open fun mapToOauth2(tokens: RestOauth1AccessToken, sourceType: String): RestOauth2AccessToken {
        // This maps the OAuth1 properties to OAuth2 for backwards compatibility
        return RestOauth2AccessToken(tokens.token, tokens.tokenSecret, Integer.MAX_VALUE,"", getExternalId(tokens, sourceType))
    }

    fun mapToList(map: MutableMap<String, String?>): String {
        val sb = StringBuilder()
        for ((key, value) in map) {
            if (value.isNullOrEmpty()) continue
            if (sb.length > 0) {
                sb.append(',')
            }
            sb.append(key).append("=\"").append(value).append('"')
        }
        return sb.toString()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OAuth1RestSourceAuthorizationService::class.java)

        const val OAUTH_CONSUMER_KEY = "oauth_consumer_key"
        const val OAUTH_NONCE = "oauth_nonce"
        const val OAUTH_SIGNATURE = "oauth_signature"
        const val OAUTH_SIGNATURE_METHOD = "oauth_signature_method"
        const val OAUTH_SIGNATURE_METHOD_VALUE = "HMAC-SHA1"
        const val OAUTH_TIMESTAMP = "oauth_timestamp"
        const val OAUTH_ACCESS_TOKEN = "oauth_token"
        const val OAUTH_VERSION = "oauth_version"
        const val OAUTH_VERSION_VALUE = "1.0"
        const val OAUTH_VERIFIER = "oauth_verifier"
        const val OAUTH_ACCESS_TOKEN_SECRET = "oauth_token_secret"
        const val OAUTH_CALLBACK = "oauth_callback"
    }

}
