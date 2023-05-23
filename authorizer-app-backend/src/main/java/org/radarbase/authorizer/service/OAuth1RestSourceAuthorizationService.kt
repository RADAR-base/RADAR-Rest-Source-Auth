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

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth1AccessToken
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.config.RestSourceClient
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
import java.util.concurrent.ThreadLocalRandom

abstract class OAuth1RestSourceAuthorizationService(
    @Context private val clientService: RestSourceClientService,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val config: AuthorizerConfig,
) : RestSourceAuthorizationService {
    private val httpClient = RestSourceAuthorizationService.httpClient()

    override suspend fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val authConfig = clientService.forSourceType(sourceType)
        logger.info("Requesting access token..")

        val payloadToken =
            RestOauth1AccessToken(payload.oauth_token!!, payload.oauth_token_secret, payload.oauth_verifier)
        val token =
            this.requestToken(authConfig.tokenEndpoint, payloadToken, sourceType) ?: throw HttpApplicationException(
                Response.Status.PROXY_AUTHENTICATION_REQUIRED,
                "user_unauthorized",
                "Access token can not be retrieved",
            )

        return token.toOAuth2(sourceType)
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? {
        return user.refreshToken?.let { RestOauth2AccessToken(it, user.refreshToken) }
    }

    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        val accessToken = user.accessToken
        if (accessToken == null || !user.authorized) {
            throw HttpBadRequestException(
                "user-already-unauthorized",
                "Cannot revoke token of unauthorized user",
            )
        }

        val authConfig = clientService.forSourceType(user.sourceType)
        return withContext(Dispatchers.IO) {
            val response = request(
                HttpMethod.Delete,
                authConfig.deregistrationEndpoint!!,
                RestOauth1AccessToken(accessToken, user.refreshToken),
                user.sourceType,
            )
            when (response.status) {
                HttpStatusCode.OK, HttpStatusCode.NoContent -> {
                    userRepository.updateToken(null, user)
                    true
                }

                HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> false
                else -> throw HttpBadGatewayException("Cannot connect to ${response.request.url}: HTTP status ${response.status}")
            }
        }
    }

    override suspend fun revokeToken(externalId: String, sourceType: String, token: String): Boolean {
        val authConfig = clientService.forSourceType(sourceType)

        if (token.isEmpty()) throw HttpBadRequestException("token-empty", "Token cannot be null or empty")
        val response = withContext(Dispatchers.IO) {
            request(
                HttpMethod.Delete,
                authConfig.deregistrationEndpoint!!,
                RestOauth1AccessToken(token, ""),
                sourceType,
            )
        }

        return when (response.status) {
            HttpStatusCode.OK, HttpStatusCode.NoContent -> true
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> false
            else -> throw HttpBadGatewayException("Cannot connect to ${response.request.url}: HTTP status ${response.status}")
        }
    }

    override suspend fun getAuthorizationEndpointWithParams(
        sourceType: String,
        userId: Long,
        state: String,
    ): String {
        logger.info("Getting auth endpoint..")
        val authConfig = clientService.forSourceType(sourceType)

        val tokens = requestToken(authConfig.preAuthorizationEndpoint, RestOauth1AccessToken(""), sourceType)

        return Url(
            authConfig.authorizationEndpoint,
            buildMap {
                put(OAUTH_ACCESS_TOKEN, tokens?.token)
                put(OAUTH_ACCESS_TOKEN_SECRET, tokens?.tokenSecret)
                put(
                    OAUTH_CALLBACK,
                    URLBuilder(config.service.callbackUrl).run {
                        parameters.append("state", state)
                        build()
                    }.toString(),
                )
            },
        ).getUrl()
    }

    private suspend fun requestToken(
        tokenEndpoint: String?,
        tokens: RestOauth1AccessToken,
        sourceType: String,
    ): RestOauth1AccessToken? = withContext(Dispatchers.IO) {
        val response = request(
            HttpMethod.Post,
            tokenEndpoint.orEmpty(),
            tokens,
            sourceType,
        )

        when (response.status) {
            HttpStatusCode.OK -> try {
                Json.decodeFromString<RestOauth1AccessToken>(response.bodyAsText().toJsonString())
            } catch (ex: IllegalArgumentException) {
                throw HttpBadGatewayException("Service did not provide a result: $ex")
            }
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> null
            else -> throw HttpBadGatewayException("Cannot connect to ${response.request.url}: HTTP status ${response.status}")
        }
    }

    suspend fun request(method: HttpMethod, url: String, tokens: RestOauth1AccessToken, sourceType: String): HttpResponse {
        val authConfig = clientService.forSourceType(sourceType)
        val params = this.getAuthParams(authConfig, tokens.token, tokens.tokenVerifier)
        params[OAUTH_SIGNATURE] =
            OauthSignature(url, params, method, authConfig.clientSecret, tokens.tokenSecret).getEncodedSignature()

        return httpClient.request(url = Url(url)) {
            headers {
                append("Authorization", "OAuth ${params.toFormattedHeader()}")
            }
            this.method = method
            if (method == HttpMethod.Post) {
                setBody("")
            }
        }
    }

    override fun signRequest(user: RestSourceUser, payload: SignRequestParams): SignRequestParams {
        val authConfig = clientService.forSourceType(user.sourceType)

        val accessToken = user.accessToken
            ?: throw HttpBadRequestException("access-token-not-found", "No access token available for user")
        val signedParams = buildMap(payload.parameters.size + 3) {
            putAll(payload.parameters)
            put(OAUTH_ACCESS_TOKEN, accessToken)
            put(OAUTH_SIGNATURE_METHOD, OAUTH_SIGNATURE_METHOD_VALUE)
            put(
                OAUTH_SIGNATURE,
                OauthSignature(
                    payload.url,
                    toSortedMap(),
                    HttpMethod.parse(payload.method),
                    authConfig.clientSecret,
                    user.refreshToken,
                ).getEncodedSignature(),
            )
        }

        return SignRequestParams(payload.url, payload.method, signedParams)
    }

    private fun getAuthParams(
        authConfig: RestSourceClient,
        accessToken: String?,
        tokenVerifier: String?,
    ): MutableMap<String, String?> = mutableMapOf(
        OAUTH_CONSUMER_KEY to authConfig.clientId,
        OAUTH_NONCE to this.generateNonce(),
        OAUTH_SIGNATURE_METHOD to OAUTH_SIGNATURE_METHOD_VALUE,
        OAUTH_TIMESTAMP to Instant.now().epochSecond.toString(),
        OAUTH_ACCESS_TOKEN to accessToken,
        OAUTH_VERIFIER to tokenVerifier,
        OAUTH_VERSION to OAUTH_VERSION_VALUE,
    )

    private fun generateNonce(): String {
        return ThreadLocalRandom.current().nextInt(1000000000).toString()
    }

    private fun String.toJsonString(): String {
        val params = this
            .replace("=", "\":\"")
            .replace("&", "\",\"")
        return "{\"$params\"}"
    }

    // This maps the OAuth1 properties to OAuth2 for backwards compatibility in the repository
    // Also, an additional request for getting the external ID is made here to pull the external id
    private suspend fun RestOauth1AccessToken.toOAuth2(sourceType: String) = RestOauth2AccessToken(
        token,
        tokenSecret,
        Integer.MAX_VALUE,
        "",
        getExternalId(sourceType),
    )

    private fun Map<String, String?>.toFormattedHeader(): String =
        entries.joinToString { (k, v) -> "$k=\"$v\"" }

    abstract suspend fun RestOauth1AccessToken.getExternalId(sourceType: String): String?

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
