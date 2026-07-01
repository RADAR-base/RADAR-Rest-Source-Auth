package org.radarbase.authorizer.service

import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * Huawei Health Kit OAuth2 authorization service.
 *
 * Key differences from standard OAuth2:
 * - Token endpoint uses client_id/client_secret as form params, not Basic Auth.
 * - redirect_uri is required in the token exchange request.
 * - Authorization URL requires access_type=offline and openid must be the first scope.
 * - External user ID is extracted from the id_token JWT (sub claim).
 * - Callback parameter from Huawei is authorization_code, not code — the frontend
 *   must map this to the code field of RequestTokenPayload before calling /authorize.
 */
class HuaweiAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, config) {

    override suspend fun getAuthorizationEndpointWithParams(
        sourceType: String,
        userId: Long,
        state: String,
    ): String {
        val authConfig = clients.forSourceType(sourceType)
        return URLBuilder().run {
            takeFrom(authConfig.authorizationEndpoint)
            parameters.append("response_type", "code")
            parameters.append("client_id", authConfig.clientId ?: "")
            parameters.append("state", state)
            // openid must be first; prepend it to whatever scopes are configured
            parameters.append("scope", prependOpenId(authConfig.scope))
            parameters.append("access_type", "offline")
            parameters.append("redirect_uri", config.service.callbackUrl.toString())
            buildString()
        }
    }

    override suspend fun requestAccessToken(
        payload: RequestTokenPayload,
        sourceType: String,
        token: String?,
    ): RestOauth2AccessToken = withContext(Dispatchers.IO) {
        val authConfig = clients.forSourceType(sourceType)
        val response = httpClient.submitForm(
            url = authConfig.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "authorization_code")
                payload.code?.let { append("code", it) }
                append("client_id", checkNotNull(authConfig.clientId))
                append("client_secret", checkNotNull(authConfig.clientSecret))
                append("redirect_uri", config.service.callbackUrl.toString())
            },
        )
        if (!response.status.isSuccess()) {
            throw HttpBadGatewayException(
                "Failed to request Huawei access token (HTTP ${response.status}): ${response.bodyAsText()}",
            )
        }
        val tokenResponse = response.body<HuaweiTokenResponse>()
        val externalUserId = tokenResponse.idToken?.let { parseSubFromJwt(it) }
        RestOauth2AccessToken(
            accessToken = tokenResponse.accessToken,
            refreshToken = tokenResponse.refreshToken,
            expiresIn = tokenResponse.expiresIn,
            tokenType = tokenResponse.tokenType,
            externalUserId = externalUserId,
        )
    }

    override suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? = withContext(Dispatchers.IO) {
        val refreshToken = user.refreshToken ?: return@withContext null
        val authConfig = clients.forSourceType(user.sourceType)
        val response = httpClient.submitForm(
            url = authConfig.tokenEndpoint,
            formParameters = Parameters.build {
                append("grant_type", "refresh_token")
                append("refresh_token", refreshToken)
                append("client_id", checkNotNull(authConfig.clientId))
                append("client_secret", checkNotNull(authConfig.clientSecret))
            },
        )
        when (response.status) {
            HttpStatusCode.OK -> {
                val tokenResponse = response.body<HuaweiTokenResponse>()
                RestOauth2AccessToken(
                    accessToken = tokenResponse.accessToken,
                    // Huawei may not return a new refresh token; keep the existing one
                    refreshToken = tokenResponse.refreshToken ?: refreshToken,
                    expiresIn = tokenResponse.expiresIn,
                    tokenType = tokenResponse.tokenType,
                    externalUserId = user.externalUserId,
                )
            }
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> {
                logger.error(
                    "Failed to refresh Huawei token (HTTP {}): {}",
                    response.status,
                    response.bodyAsText(),
                )
                null
            }
            else -> throw HttpBadGatewayException(
                "Cannot connect to Huawei token endpoint (HTTP ${response.status}): ${response.bodyAsText()}",
            )
        }
    }

    override suspend fun revokeToken(user: RestSourceUser): Boolean {
        // Huawei does not expose a public token revocation endpoint
        logger.info("Token revocation not supported for Huawei; marking as revoked locally")
        return true
    }

    /** Decodes the JWT payload (no signature verification needed) and extracts the sub claim. */
    private fun parseSubFromJwt(idToken: String): String? {
        return try {
            val payloadBase64 = idToken.split(".").getOrNull(1) ?: return null
            // Add padding so Base64 decoder doesn't complain
            val padded = payloadBase64 + "=".repeat((4 - payloadBase64.length % 4) % 4)
            val decoded = Base64.getUrlDecoder().decode(padded)
            val element = Json.parseToJsonElement(String(decoded, Charsets.UTF_8))
            element.jsonObject["sub"]?.jsonPrimitive?.content
        } catch (e: Exception) {
            logger.warn("Failed to parse sub from Huawei id_token: {}", e.toString())
            null
        }
    }

    /** Ensures openid is the first scope, then appends the rest. */
    private fun prependOpenId(scope: String?): String {
        val trimmed = scope?.trim().orEmpty()
        val parts = trimmed.split(Regex("\\s+")).filter { it.isNotEmpty() && it != "openid" }
        return buildString {
            append("openid")
            if (parts.isNotEmpty()) {
                append(" ")
                append(parts.joinToString(" "))
            }
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(HuaweiAuthorizationService::class.java)
    }

    @Serializable
    private data class HuaweiTokenResponse(
        @SerialName("access_token") val accessToken: String,
        @SerialName("refresh_token") val refreshToken: String? = null,
        @SerialName("expires_in") val expiresIn: Int = 3600,
        @SerialName("token_type") val tokenType: String? = null,
        @SerialName("id_token") val idToken: String? = null,
    )
}
