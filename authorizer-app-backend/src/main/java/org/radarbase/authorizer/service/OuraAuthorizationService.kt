package org.radarbase.authorizer.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriBuilder
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.util.request
import org.radarbase.jersey.util.requestJson
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class OuraAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, httpClient, objectMapper, config) {

    override fun revokeToken(user: RestSourceUser): Boolean {
        val accessToken = user.accessToken ?: run {
            logger.error("Cannot revoke token of user {} without an access token", user.userId)
            return false
        }
        // revoke token using the deregistrationEndpoint token endpoint
        val authConfig = clients.forSourceType(user.sourceType)
        val deregistrationEndpoint = checkNotNull(authConfig.deregistrationEndpoint)

        val revokeURI = UriBuilder.fromUri(deregistrationEndpoint)
                    .queryParam("access_token", accessToken)
                    .build()
                    .toString()

        val credentials = Credentials.basic(
            checkNotNull(authConfig.clientId),
            checkNotNull(authConfig.clientSecret),
        )
        val form = FormBody.Builder().add("token", accessToken).build()

        var requestObj = Request.Builder().apply {
            url(revokeURI)
        }.post(form).build()
        val response = httpClient.request(requestObj)
        if (response) {
            logger.info("Successfully revoked token for user {}", user.userId)
        } else {
            logger.error("Failed to revoke token for user {}", user.userId)
            return false
        }
        return httpClient.request(post(form, user.sourceType))
    }
}