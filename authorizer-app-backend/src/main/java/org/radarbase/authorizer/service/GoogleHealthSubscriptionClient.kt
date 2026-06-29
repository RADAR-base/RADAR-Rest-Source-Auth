/*
 *  Copyright 2026 King's College London
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

import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import jakarta.ws.rs.core.Context
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.model.RemoteSubscription
import org.radarbase.authorizer.model.SubscriptionResult
import org.slf4j.LoggerFactory
import java.io.IOException
import java.security.MessageDigest

class GoogleHealthSubscriptionClient(
    @param:Context private val config: AuthorizerConfig,
    @param:Context private val tokenProvider: GoogleServiceAccountTokenProvider,
) {
    private val ghConfig = config.googleHealth
    private val projectId = ghConfig.googleCloudProjectId
    private val subscriberId = ghConfig.subscriberId
    private val baseUrl = ghConfig.apiBaseUrl.trimEnd('/')
    private val defaultDataTypes = ghConfig.dataTypes
    private val httpClient = RestSourceAuthorizationService.httpClient()

    val isConfigured: Boolean
        get() = tokenProvider.isConfigured

    suspend fun createSubscription(
        healthUserId: String,
        dataTypes: List<String> = defaultDataTypes,
    ): SubscriptionResult {
        val subscriptionId = subscriptionIdFor(healthUserId)
        val url = "$baseUrl/projects/$projectId/subscribers/$subscriberId/subscriptions?subscriptionId=$subscriptionId"
        val payload = buildJsonObject {
            put("user", userResourceName(healthUserId))
            putJsonArray("dataTypes") { dataTypes.forEach { add(it) } }
        }
        return execute(healthUserId, "create") { token ->
            httpClient.post(url) {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
        }
    }

    suspend fun deleteSubscription(healthUserId: String): SubscriptionResult {
        val subscriptionId = subscriptionIdFor(healthUserId)
        val url = "$baseUrl/projects/$projectId/subscribers/$subscriberId/subscriptions/$subscriptionId"
        return execute(healthUserId, "delete") { token ->
            httpClient.delete(url) {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
            }
        }
    }

    /**
     * Deletes a subscription by its full Google resource name (as returned by [listSubscriptions]).
     */
    suspend fun deleteByName(name: String): SubscriptionResult =
        execute(name, "delete") { token ->
            httpClient.delete("$baseUrl/$name") {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
            }
        }

    suspend fun patchSubscription(name: String, dataTypes: List<String>): SubscriptionResult {
        val url = "$baseUrl/$name?updateMask=dataTypes"
        val payload = buildJsonObject {
            putJsonArray("dataTypes") { dataTypes.forEach { add(it) } }
        }
        return execute(name, "patch") { token ->
            httpClient.patch(url) {
                headers { append(HttpHeaders.Authorization, "Bearer $token") }
                contentType(ContentType.Application.Json)
                setBody(payload.toString())
            }
        }
    }

    /**
     * Returns every subscription under this deployment's subscriber (following all pages).
     *
     * Throws on any error instead of returning an empty list, so callers treat a failed read as
     * "unknown" rather than "Google has no subscriptions".
     */
    @Throws(IOException::class)
    suspend fun listSubscriptions(): List<RemoteSubscription> {
        check(tokenProvider.isConfigured) { "Service account not configured" }
        val token = tokenProvider.getAccessToken()
        val result = mutableListOf<RemoteSubscription>()
        var pageToken: String? = null
        do {
            val response = httpClient.get(
                "$baseUrl/projects/$projectId/subscribers/$subscriberId/subscriptions",
            ) {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $token")
                    append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                }
                parameter("pageSize", PAGE_SIZE)
                if (!pageToken.isNullOrEmpty()) parameter("pageToken", pageToken)
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                throw IOException(
                    "List subscriptions failed for subscriber $subscriberId: HTTP ${response.status} - $body",
                )
            }
            val tree = if (body.isEmpty()) JsonObject(emptyMap()) else json.parseToJsonElement(body).jsonObject
            tree["subscriptions"]?.jsonArray?.forEach { node -> result += parseSubscription(node.jsonObject) }
            pageToken = tree["nextPageToken"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotEmpty() }
        } while (pageToken != null)
        return result
    }

    private suspend fun execute(
        identifier: String,
        action: String,
        sendRequest: suspend (String) -> HttpResponse,
    ): SubscriptionResult {
        if (!tokenProvider.isConfigured) return SubscriptionResult.NotConfigured
        val token = try {
            tokenProvider.getAccessToken()
        } catch (ex: Exception) {
            logger.warn("Could not obtain service-account token for {} of {}", action, identifier, ex)
            return SubscriptionResult.TransientFailure("token: ${ex.message}")
        }
        return try {
            val response = sendRequest(token)
            val body = response.bodyAsText()
            val code = response.status.value
            when {
                response.status.isSuccess() -> {
                    val name = runCatching {
                        json.parseToJsonElement(body).jsonObject["name"]?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    logger.info("Subscription {} ok for {} (name={})", action, identifier, name)
                    SubscriptionResult.Success(name)
                }
                // create: subscription already present. delete: subscription already gone.
                code == 409 || code == 404 -> SubscriptionResult.Success(null)

                code == 429 || code in 500..599 -> {
                    logger.warn("Transient {} failure for {}: HTTP {} - {}", action, identifier, code, body)
                    SubscriptionResult.TransientFailure("HTTP $code")
                }

                else -> {
                    logger.error("Permanent {} failure for {}: HTTP {} - {}", action, identifier, code, body)
                    SubscriptionResult.PermanentFailure(code, body)
                }
            }
        } catch (ex: IOException) {
            logger.warn("I/O error during subscription {} for {}", action, identifier, ex)
            SubscriptionResult.TransientFailure("io: ${ex.message}")
        }
    }

    private fun parseSubscription(node: JsonObject): RemoteSubscription = RemoteSubscription(
        name = node["name"]?.jsonPrimitive?.contentOrNull ?: "",
        user = node["user"]?.jsonPrimitive?.contentOrNull ?: "",
        dataTypes = node["dataTypes"]?.jsonArray?.mapNotNull { it.jsonPrimitive.contentOrNull } ?: emptyList(),
    )

    private fun userResourceName(healthUserId: String): String = "users/$healthUserId"

    /**
     * Subscription id derived from the health user id. We hash rather than use the raw id because the
     * health user id's character set and length are not guaranteed to satisfy Google's resource-id rules.
     */
    fun subscriptionIdFor(healthUserId: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(healthUserId.toByteArray())
        val hex = digest.joinToString("") { "%02x".format(it) }
        return "u" + hex.take(SUBSCRIPTION_ID_HEX_LEN)
    }

    companion object {
        private const val PAGE_SIZE = 1000
        private const val SUBSCRIPTION_ID_HEX_LEN = 32
        private val json = Json { ignoreUnknownKeys = true }
        private val logger = LoggerFactory.getLogger(GoogleHealthSubscriptionClient::class.java)
    }
}
