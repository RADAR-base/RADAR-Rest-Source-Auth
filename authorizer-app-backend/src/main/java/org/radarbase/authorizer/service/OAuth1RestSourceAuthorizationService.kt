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
import org.radarbase.authorizer.api.RestOauth1AccessToken
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.security.SecureRandom
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

    override fun requestAccessToken(payload: Any, sourceType: String): RestOauth1AccessToken? {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        var accessToken = payload
        var tokenVerifier = payload
        var params = this.getCommonAuthParamsBuilder(authConfig)
                .queryParam("oauth_token", accessToken)
                .queryParam("oauth_verifier", tokenVerifier)
        val tokens = this.requestToken(params, authConfig.tokenEndpoint, authConfig.clientSecret)
        logger.info(tokens.toString())

        return tokens
    }

    override fun refreshToken(refreshToken: String, sourceType: String): RestOauth1AccessToken? {
       // TODO
        return null
    }

    override fun revokeToken(accessToken: String, sourceType: String): Boolean {
        // TODO
        return false
    }

    fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String {
        val authConfig = configMap[sourceType]
                ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        var params = this.getCommonAuthParamsBuilder(authConfig)
        val tokens = this.requestToken(params, authConfig.preAuthorizationEndpoint, authConfig.clientSecret)
        logger.info(tokens.toString())
        return UriBuilder.fromUri(authConfig.authorizationEndpoint)
                .queryParam("oauth_token", tokens?.accessToken)
                .queryParam(("oauth_callback"), callBackUrl)
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
        val method = "POST";
        val paramsString = params.build().toString().substring(1)
        val signatureBaseBuilder = StringBuilder()
        signatureBaseBuilder.append(method)
                .append("&")
                .append(URLEncoder.encode(url))
                .append("&")
                .append(URLEncoder.encode(paramsString))
        val signatureBase = signatureBaseBuilder.toString()
        val key = clientSecret + "&";
        val signatureEncoded = URLEncoder.encode(this.encodeSHA(key, signatureBase))
        return signatureEncoded;
    }

    fun generateNonce(): String {
        val secureRandom = SecureRandom()
        val stringBuilder = java.lang.StringBuilder()
        for (i in 0..10) {
            stringBuilder.append(secureRandom.nextInt(10))
        }
        return stringBuilder.toString()
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

    companion object {
        val logger: Logger = LoggerFactory.getLogger(OAuth2RestSourceAuthorizationService::class.java)
    }
}
