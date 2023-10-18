package org.radarbase.authorizer.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriBuilder
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.radarbase.authorizer.api.OuraAuthUserId
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.util.request
import org.radarbase.jersey.util.requestJson

class OuraAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, httpClient, objectMapper, config) {
    private val tokenReader = objectMapper.readerFor(RestOauth2AccessToken::class.java)
    private val oauthUserReader = objectMapper.readerFor(OuraAuthUserId::class.java)

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val authorizationConfig = clients.forSourceType(sourceType)
        val clientId = checkNotNull(authorizationConfig.clientId)

        val form = FormBody.Builder().apply {
            payload.code?.let { add("code", it) }
            add("grant_type", "authorization_code")
            add("client_id", clientId)
            add("redirect_uri", config.service.callbackUrl.toString())
        }.build()
        val accessToken: RestOauth2AccessToken = httpClient.requestJson(post(form, sourceType), tokenReader)
        if (accessToken.accessToken == null) {
            logger.error("Failed to get access token for user {}", clientId)
            throw HttpBadGatewayException("Service ${sourceType} did not provide a result")
        }
        val ouraUserUri = UriBuilder.fromUri(Oura_USER_ID_ENDPOINT).queryParam("access_token", accessToken.accessToken).build().toString()
        val userReq = Request.Builder().apply {
            url(ouraUserUri)
        }.build()
        val userIdObj: OuraAuthUserId = httpClient.requestJson(userReq, oauthUserReader)
        if (userIdObj.userId == null) {
            logger.error("Failed to get user id for user {}", clientId)
            throw HttpBadGatewayException("Service ${sourceType} did not provide a result")
        }
        val userId = userIdObj.userId
        return accessToken.copy(externalUserId = userId)
    }

    override fun revokeToken(user: RestSourceUser): Boolean {
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return false
        }
        // revoke token using the deregistrationEndpoint token endpoint
        val authConfig = clients.forSourceType(user.sourceType)
        val deregistrationEndpoint = checkNotNull(authConfig.deregistrationEndpoint)

        val revokeURI = UriBuilder.fromUri(deregistrationEndpoint).queryParam("access_token", accessToken).build().toString()

        var requestObj = Request.Builder().apply {
            url(revokeURI)
            post(FormBody.Builder().build())
        }.build()

        val response = httpClient.request(requestObj)
        if (response) {
            logger.info("Successfully revoked token for user {}", user.userId)
            return true
        } else {
            logger.error("Failed to revoke token for user {}", user.userId)
            return false
        }
    }
    companion object {
        private const val Oura_USER_ID_ENDPOINT = "https://api.ouraring.com/v1/userinfo?"
    }
}
