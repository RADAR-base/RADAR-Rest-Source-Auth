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
import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.OauthSignature
import org.radarbase.authorizer.util.Url
import org.radarbase.jersey.exception.HttpApplicationException
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant
import javax.ws.rs.core.Context
import javax.ws.rs.core.Response

abstract class OAuth1RestSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
) : RestSourceAuthorizationService {
    private val configMap = restSourceClients.clients.map { it.sourceType to it }.toMap()
    private val tokenReader = objectMapper.readerFor(RestOauth1AccessToken::class.java)

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val authConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found",
                "Cannot find client configurations for source-type $sourceType")
        logger.info("Requesting access token..")

        val payloadToken =
            RestOauth1AccessToken(payload.oauth_token!!, payload.oauth_token_secret, payload.oauth_verifier)
        val token =
            this.requestToken(authConfig.tokenEndpoint, payloadToken, sourceType) ?: throw HttpApplicationException(
                Response.Status.PROXY_AUTHENTICATION_REQUIRED,
                "user_unauthorized",
                "Access token can not be retrieved"
            )

        return token.toOAuth2(sourceType)
    }

    override fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? {
        return user.refreshToken?.let { RestOauth2AccessToken(it, user.refreshToken) }
    }

    override fun revokeToken(user: RestSourceUser): Boolean {
        val userId = user.id ?: return false
        val accessToken = user.accessToken
        if (accessToken == null || !user.authorized) return false

        val authConfig = configMap[user.sourceType]
            ?: throw HttpBadRequestException("client-config-not-found",
                "Cannot find client configurations for source-type ${user.sourceType}")
        val req = createRequest("DELETE",
            authConfig.deregistrationEndpoint!!,
            RestOauth1AccessToken(accessToken, user.refreshToken),
            user.sourceType)

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200, 204 -> !this.userRepository.updateToken(null, userId).authorized
                400, 401, 403 -> false
                else -> throw HttpBadGatewayException("Cannot connect to ${authConfig.deregistrationEndpoint}: HTTP status ${response.code}")
            }
        }
    }

    override fun revokeToken(externalId: String, sourceType: String, token: String): Boolean {
        val authConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found",
                "Cannot find client configurations for source-type ${sourceType}")

        if(token.isNullOrEmpty()) throw HttpBadRequestException("token-empty", "Token cannot be null or empty")
        val req = createRequest("DELETE",
            authConfig.deregistrationEndpoint!!,
            RestOauth1AccessToken(token, ""),
            sourceType)

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200, 204 -> true
                400, 401, 403 -> false
                else -> throw HttpBadGatewayException("Cannot connect to ${authConfig.deregistrationEndpoint}: HTTP status ${response.code}")
            }
        }
    }

    override fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String {
        logger.info("Getting auth endpoint..")
        val authConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found",
                "Cannot find client configurations for source-type $sourceType")

        val tokens = this.requestToken(authConfig.preAuthorizationEndpoint, RestOauth1AccessToken(""), sourceType)
        val params = mapOf<String, String?>(
            OAUTH_ACCESS_TOKEN to tokens?.token,
            OAUTH_ACCESS_TOKEN_SECRET to tokens?.tokenSecret,
            OAUTH_CALLBACK to callBackUrl
        )

        return Url(authConfig.authorizationEndpoint, params).getUrl()
    }

    fun requestToken(
        tokenEndpoint: String?,
        tokens: RestOauth1AccessToken,
        sourceType: String,
    ): RestOauth1AccessToken? {
        val req = createRequest("POST", tokenEndpoint.orEmpty(), tokens, sourceType)

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200 -> response.body?.string()
                    ?.let { tokenReader.readValue<RestOauth1AccessToken>(parseParams(it)) }
                    ?: throw HttpBadGatewayException("Service did not provide a result")
                400, 401, 403 -> null
                else -> throw HttpBadGatewayException("Cannot connect to ${tokenEndpoint}: HTTP status ${response.code}")
            }
        }
    }

    fun createRequest(method: String, url: String, tokens: RestOauth1AccessToken, sourceType: String): Request {
        val authConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found",
                "Cannot find client configurations for source-type ${sourceType}")
        var params = this.getAuthParams(authConfig, tokens.token, tokens.tokenVerifier)
        params[OAUTH_SIGNATURE] =
            OauthSignature(url, params, method, authConfig.clientSecret, tokens.tokenSecret).getEncodedSignature()
        val headers = params.toFormattedHeader()

        return Request.Builder()
            .url(url)
            .header("Authorization", "OAuth $headers")
            .method(method, if (method == "POST") RequestBody.create(null, "") else null)
            .build()
    }

    override fun signRequest(user: RestSourceUser, payload: SignRequestParams): SignRequestParams {
        val authConfig = configMap[user.sourceType]
            ?: throw HttpBadRequestException("client-config-not-found",
                "Cannot find client configurations for source-type ${user.sourceType}")

        val accessToken = user.accessToken
            ?: throw HttpBadRequestException("access-token-not-found", "No access token available for user")
        val signedParams = payload.parameters.toMutableMap()
        signedParams[OAUTH_ACCESS_TOKEN] = accessToken
        signedParams[OAUTH_SIGNATURE_METHOD] = OAUTH_SIGNATURE_METHOD_VALUE
        signedParams[OAUTH_SIGNATURE] = OauthSignature(payload.url,
            signedParams.toSortedMap(),
            payload.method,
            authConfig.clientSecret,
            user.refreshToken).getEncodedSignature()

        return SignRequestParams(payload.url, payload.method, signedParams)
    }

    private fun getAuthParams(
        authConfig: RestSourceClient,
        accessToken: String?,
        tokenVerifier: String?,
    ): MutableMap<String, String?> {
        return mutableMapOf<String, String?>(
            OAUTH_CONSUMER_KEY to authConfig.clientId,
            OAUTH_NONCE to this.generateNonce(),
            OAUTH_SIGNATURE_METHOD to OAUTH_SIGNATURE_METHOD_VALUE,
            OAUTH_TIMESTAMP to Instant.now().epochSecond.toString(),
            OAUTH_ACCESS_TOKEN to accessToken,
            OAUTH_VERIFIER to tokenVerifier,
            OAUTH_VERSION to OAUTH_VERSION_VALUE)
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

    fun RestOauth1AccessToken.toOAuth2(sourceType: String): RestOauth2AccessToken {
        // This maps the OAuth1 properties to OAuth2 for backwards compatibility in the repository
        // Also, an additional request for getting the external ID is made here to pull the external id
        val tokens = this
        return RestOauth2AccessToken(tokens.token,
            tokens.tokenSecret,
            Integer.MAX_VALUE,
            "",
            tokens.getExternalId(sourceType))
    }

    fun Map<String, String?>.toFormattedHeader(): String = this.map { (k, v) -> "$k=\"$v\"" }.joinToString()

    abstract fun RestOauth1AccessToken.getExternalId(sourceType: String): String?


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
