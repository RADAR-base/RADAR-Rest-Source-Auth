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

class OuraAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, httpClient, objectMapper, config) {
    private val oauthUserReader = objectMapper.readerFor(OuraAuthUserId::class.java)

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val authorizationConfig = clients.forSourceType(sourceType)
        val clientId = checkNotNull(authorizationConfig.clientId)
        val accessToken: RestOauth2AccessToken = super.requestAccessToken(payload, sourceType)
        if (accessToken.accessToken == null) {
            logger.error("Failed to get access token for user {}", clientId)
            throw HttpBadGatewayException("Service $sourceType did not provide a result")
        }
        return accessToken.copy(externalUserId = getExternalId(accessToken.accessToken))
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

    private fun getExternalId(accessToken: String): String {
        val ouraUserUri = UriBuilder.fromUri(OURA_USER_ID_ENDPOINT).queryParam("access_token", accessToken).build().toString()
        val userReq = Request.Builder().apply {
            url(ouraUserUri)
        }.build()
        return httpClient.newCall(userReq)
            .execute()
            .use { response ->
                when (response.code) {
                    200 -> response.body?.byteStream()
                        ?.let {
                            oauthUserReader.readValue<OuraAuthUserId>(it).userId
                        }
                        ?: throw HttpBadGatewayException("Service did not provide a result")
                    400, 401, 403 -> throw HttpBadGatewayException("Service was unable to fetch the external ID")
                    else -> throw HttpBadGatewayException("Cannot connect to $OURA_USER_ID_ENDPOINT: HTTP status ${response.code}")
                }
            }
    }

    companion object {
        private const val OURA_USER_ID_ENDPOINT = "https://api.ouraring.com/v1/userinfo?"
    }
}
