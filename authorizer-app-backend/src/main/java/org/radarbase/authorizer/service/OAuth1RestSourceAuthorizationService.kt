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
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.time.Instant
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.ws.rs.core.Context
import javax.ws.rs.core.UriBuilder


class OAuth1RestSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper
): RestSourceAuthorizationService(restSourceClients, httpClient, objectMapper) {
    private val configMap = restSourceClients.clients.map { it.sourceType to it }.toMap()
    private val tokenReader = objectMapper.readerFor(RestOauth1AccessToken::class.java)

    private val OAUTH_CONSUMER_KEY = "oauth_consumer_key"
    private val OAUTH_NONCE = "oauth_nonce"
    private val OAUTH_SIGNATURE = "oauth_signature"
    private val OAUTH_SIGNATURE_METHOD = "oauth_signature_method"
    private val OAUTH_SIGNATURE_METHOD_VALUE = "HMAC-SHA1"
    private val OAUTH_TIMESTAMP = "oauth_timestamp"
    private val OAUTH_ACCESS_TOKEN = "oauth_token"
    private val OAUTH_VERSION = "oauth_version"
    private val OAUTH_VERSION_VALUE = "1.0"
    private val OAUTH_VERIFIER = "oauth_verifier"
    private val OAUTH_ACCESS_TOKEN_SECRET = "oauth_token_secret"
    private val OAUTH_CALLBACK = "oauth_callback"

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken? {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")
        logger.info("Requesting access token..")
        var params = this.getCommonAuthParamsBuilder(authConfig)
                .queryParam(OAUTH_ACCESS_TOKEN, payload.oauth_token)
                .queryParam(OAUTH_VERIFIER, payload.oauth_verifier)
                .queryParam(OAUTH_VERSION, OAUTH_VERSION_VALUE)

        val tokens = this.requestToken(params, authConfig.tokenEndpoint, authConfig.clientSecret, payload.oauth_token_secret)
        return tokens?.let { mapToOauth2(it, sourceType) }
    }

    override fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? {
        return user.accessToken?.let { RestOauth2AccessToken(it, user.refreshToken) }
    }

    override fun revokeToken(user: RestSourceUser): Boolean {
        val authConfig = configMap[user.sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type ${user.sourceType}")
        val url = "https://healthapi.garmin.com/wellness-api/rest/user/registration"

        var params = this.getCommonAuthParamsBuilder(authConfig)
                .queryParam(OAUTH_ACCESS_TOKEN, user.accessToken)
                .queryParam(OAUTH_VERSION, OAUTH_VERSION_VALUE)
        val signature = this.getOAuthSignature("POST", params.clone(), url, authConfig.clientSecret, user.refreshToken)
        val headers = this.getPreAuthHeaders(params, signature)
        val urlWithParams = UriBuilder.fromUri(url).replaceQuery(headers).build().toString()

        val req: Request = Request.Builder()
                .url(urlWithParams)
                .header("Authorization", headers)
                .method("DELETE", null)
                .build()

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200 -> true
                400, 401, 403 -> false
                else -> throw HttpBadGatewayException("Cannot connect to ${url}: HTTP status ${response.code}")
            }
        }
    }

    override fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        var params = this.getCommonAuthParamsBuilder(authConfig).queryParam("oauth_version", "1.0");
        val tokens = this.requestToken(params, authConfig.preAuthorizationEndpoint, authConfig.clientSecret, "")
        return UriBuilder.fromUri(authConfig.authorizationEndpoint)
                .queryParam(OAUTH_ACCESS_TOKEN, tokens?.token)
                .queryParam(OAUTH_ACCESS_TOKEN_SECRET, tokens?.tokenSecret)
                .queryParam(OAUTH_CALLBACK, callBackUrl)
                .build().toString()
    }

    fun requestToken(params: UriBuilder, tokenEndpoint: String?, clientSecret: String?, tokenSecret: String?): RestOauth1AccessToken? {
        val url = tokenEndpoint
        val signature = this.getOAuthSignature("POST", params.clone(), url, clientSecret, tokenSecret)
        val headers = this.getPreAuthHeaders(params, signature)

        val urlWithParams = UriBuilder.fromUri(url).replaceQuery(headers).build().toString()
        val req: Request = Request.Builder()
                .url(urlWithParams)
                .method("POST", RequestBody.create(null, ""))
                .build()

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200 -> response.body?.string()
                        ?.let { tokenReader.readValue<RestOauth1AccessToken>(parseParams(it)) }
                        ?: throw HttpBadGatewayException("Service did not provide a result")
                400, 401, 403 -> null
                else -> throw HttpBadGatewayException("Cannot connect to ${url}: HTTP status ${response.code}")
            }
        }
    }

    fun getPreAuthHeaders(params: UriBuilder, signature: String): String {
        var headerParams = params.queryParam(OAUTH_SIGNATURE, signature)
        var headers = headerParams.build().toString().substring((1))
        return headers;
    }

    fun getCommonAuthParamsBuilder(authConfig: RestSourceClient): UriBuilder {
        val time = Instant.now().epochSecond
        val nonce = this.generateNonce()
        return UriBuilder.fromUri("")
                .queryParam(OAUTH_CONSUMER_KEY, authConfig.clientId)
                .queryParam(OAUTH_NONCE, nonce)
                .queryParam(OAUTH_SIGNATURE_METHOD, OAUTH_SIGNATURE_METHOD_VALUE)
                .queryParam(OAUTH_TIMESTAMP, time)
    }

    fun getOAuthSignature(method: String, params: UriBuilder, url: String?, clientSecret: String?, tokenSecret: String?): String {
        val encodedUrl = URLEncoder.encode(url)
        val encodedParams = URLEncoder.encode(params.build().toString().substring(1))
        var signatureBase = "$method&$encodedUrl&$encodedParams"
        var key = "$clientSecret&$tokenSecret"
        val signatureEncoded = URLEncoder.encode(this.encodeSHA(key, signatureBase))
        return signatureEncoded;
    }

    fun generateNonce(): Int {
        return Math.floor(Math.random() * 1000000000).toInt();
    }

    fun encodeSHA(key: String, plaintext: String): String?{
        val result: String;
        val signingKey = SecretKeySpec(key.toByteArray(),"HmacSHA1");
        val mac = Mac.getInstance("HmacSHA1");
        mac.init(signingKey);
        val rawHmac= mac.doFinal(plaintext.toByteArray());
        result = Base64.getEncoder().encodeToString(rawHmac);
        return result;
    }

    fun parseParams(input: String): String? {
        var params = input
        params = params.replace("=".toRegex(), "\":\"")
        params = params.replace("&".toRegex(), "\",\"")
        return "{\"$params\"}"
    }

    fun mapToOauth2(tokens: RestOauth1AccessToken, sourceType: String): RestOauth2AccessToken {
        return RestOauth2AccessToken(tokens.token, tokens.tokenSecret, Integer.MAX_VALUE,"", getExternalId(tokens, sourceType))
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OAuth2RestSourceAuthorizationService::class.java)
    }


    fun getExternalId(tokens: RestOauth1AccessToken, sourceType: String): String? {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type ${sourceType}")
        val url = "https://healthapi.garmin.com/wellness-api/rest/user/id"

        var params = this.getCommonAuthParamsBuilder(authConfig)
                .queryParam("oauth_token", tokens.token)
                .queryParam("oauth_version", "1.0")
        val signature = this.getOAuthSignature("GET", params.clone(), url, authConfig.clientSecret, tokens.tokenSecret)
        val headers = this.getPreAuthHeaders(params, signature).replace("&", "\",").replace("=", "=\"").plus("\"")

        val req: Request = Request.Builder()
                .url(url)
                .header("Authorization", "OAuth $headers")
                .header("Accept", "application/json")
                .method("GET", null)
                .build()

        return httpClient.newCall(req).execute().use { response ->
            when (response.code) {
                200 -> response.body?.byteStream()
                        ?.let {  objectMapper.readerFor(RestOauth1UserId::class.java).readValue<RestOauth1UserId>(it).userId }
                        ?: throw HttpBadGatewayException("Service did not provide a result")
                400, 401, 403 -> null
                else -> throw HttpBadGatewayException("Cannot connect to ${url}: HTTP status ${response.code}")
            }
        }
    }



}
