package org.radarbase.authorizer.service


import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import jakarta.ws.rs.core.Context
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.radarbase.authorizer.api.HuaweiUserId          
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.service.OAuth2RestSourceAuthorizationService
import org.radarbase.jersey.exception.HttpBadGatewayException

class HuaweiAuthorizationService(
    @Context private val clients: RestSourceClientService,
    @Context private val config: AuthorizerConfig,
) : OAuth2RestSourceAuthorizationService(clients, config) {

    override suspend fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken {
        val accessToken: RestOauth2AccessToken = super.requestAccessToken(payload, sourceType)
        return accessToken.copy(externalUserId = getExternalId(accessToken.accessToken))
    }

    // override suspend fun revokeToken(user: RestSourceUser): Boolean {
    //     // Huawei OAuth2 deregistration/revocation
    //     val accessToken = user.accessToken ?: run {
    //         logger.error("Cannot revoke token of user {} without an access token", user.userId)
    //         return false
    //     }
        
    //     val authConfig = clients.forSourceType(user.sourceType)
    //     val revokeEndpoint = authConfig.deregistrationEndpoint ?: run {
    //         logger.warn("No revocation endpoint configured for Huawei")
    //         return true // Treat as success if no endpoint configured
    //     }

    //     return try {
    //         withContext(Dispatchers.IO) {
    //             val response = httpClient.submitForm {
    //                 url {
    //                     takeFrom(revokeEndpoint)
    //                 }
    //                 parameter("access_token", accessToken)
    //             }
    //             response.status.isSuccess()
    //         }
    //     } catch (ex: Exception) {
    //         logger.error("Failed to revoke token: {}", ex.message)
    //         false
    //     }
    // }

    private suspend fun getExternalId(accessToken: String): String = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get {
                url(HUAWEI_USER_ID_ENDPOINT)
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }
            if (response.status.isSuccess()) {
                response.body<HuaweiUserId>().userId
            } else {
                logger.error(
                    "Unable to fetch data from Oura $HUAWEI_USER_ID_ENDPOINT (Http Status {}): {}",
                    response.status,
                    response.bodyAsText().take(512),
                )
                throw HttpBadGatewayException("Cannot connect to $HUAWEI_USER_ID_ENDPOINT: HTTP status ${response.status}")
            }
        } catch (ex: IOException) {
            logger.error("Unable to fetch data from Oura $HUAWEI_USER_ID_ENDPOINT: {}", ex.toString())
            throw HttpBadGatewayException("Cannot connect to $HUAWEI_USER_ID_ENDPOINT: I/O error")
        }
    }

    companion object {
        private const val HUAWEI_USER_ID_ENDPOINT = "https://api.huawei.com/v2/usercollection/personal_info"
    }
}