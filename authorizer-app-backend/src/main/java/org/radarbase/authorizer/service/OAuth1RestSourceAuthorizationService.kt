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
import org.radarbase.authorizer.api.RestOauth2AccessToken
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

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken? {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        var params = this.getCommonAuthParamsBuilder(authConfig)
                .queryParam("oauth_token", payload.oauth_token)
                .queryParam("oauth_verifier", payload.oauth_verifier)

        val tokens = this.requestToken(params, authConfig.tokenEndpoint, authConfig.clientSecret)
        logger.info("Requesting access token..")
        return tokens?.let { mapToOauth2(it) }
    }

    override fun refreshToken(refreshToken: String, sourceType: String): RestOauth2AccessToken? {
       // TODO
        return null
    }

    override fun revokeToken(accessToken: String, sourceType: String): Boolean {
        // TODO
        return false
    }

    override fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        var params = this.getCommonAuthParamsBuilder(authConfig)
        val tokens = this.requestToken(params, authConfig.preAuthorizationEndpoint, authConfig.clientSecret)
        return UriBuilder.fromUri(authConfig.authorizationEndpoint)
                .queryParam("oauth_token", tokens?.token)
                .queryParam("oauth_callback", callBackUrl)
                .build().toString()
    }

    fun requestToken(params: UriBuilder, tokenEndpoint: String?, clientSecret: String?): RestOauth1AccessToken? {
        val url = tokenEndpoint
        val signature = this.getOAuthSignature(params.clone(), url, clientSecret)
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
        var headerParams = params.queryParam("oauth_signature", signature)
        var headers = headerParams.build().toString().substring((1))
        return headers;
    }

    fun getCommonAuthParamsBuilder(authConfig: RestSourceClient): UriBuilder {
        val time = Instant.now().epochSecond
        val nonce = this.generateNonce()
        return UriBuilder.fromUri("")
                .queryParam("oauth_consumer_key", authConfig.clientId)
                .queryParam("oauth_nonce", nonce)
                .queryParam("oauth_signature_method", "HMAC-SHA1")
                .queryParam("oauth_timestamp", time)
                .queryParam("oauth_version", "1.0");
    }

    fun getOAuthSignature(params: UriBuilder, url: String?, clientSecret: String?): String {
        val method = "POST"
        val encodedUrl = URLEncoder.encode(url)
        val encodedParams = URLEncoder.encode(params.build().toString().substring(1))
        var signatureBase = "$method&$encodedUrl&$encodedParams"
        var key = "$clientSecret&"
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
          val rawHmac=mac.doFinal(plaintext.toByteArray());
          result = Base64.getEncoder().encodeToString(rawHmac);
        return result;
    }

    fun parseParams(input: String): String? {
        var params = input
        params = params.replace("=".toRegex(), "\":\"")
        params = params.replace("&".toRegex(), "\",\"")
        return "{\"$params\"}"
    }

    fun mapToOauth2(tokens: RestOauth1AccessToken): RestOauth2AccessToken {
        return RestOauth2AccessToken(tokens.token, tokens.tokenSecret)
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OAuth2RestSourceAuthorizationService::class.java)
    }
}
