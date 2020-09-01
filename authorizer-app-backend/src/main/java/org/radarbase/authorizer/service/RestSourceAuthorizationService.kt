package org.radarbase.authorizer.service

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context

class RestSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper
) {

    private val configMap = restSourceClients.clients.map { it.sourceType to it }.toMap()

    fun requestAccessToken(code: String, sourceType: String): RestOauth2AccessToken {
        val authorizationConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        val form = FormBody.Builder()
            .add("code", code)
            .add("grant_type", "authorization_code")
            .add("client_id", authorizationConfig.clientId)
            .build();
        logger.info("Requesting access token with authorization code")
        return objectMapper.readValue(execute(post(form, sourceType)), RestOauth2AccessToken::class.java)
    }


    fun refreshToken(refreshToken: String, sourceType: String): RestOauth2AccessToken {
        val form = FormBody.Builder()
            .add("grant_type", "refresh_token")
            .add("refresh_token", refreshToken)
            .build();
        logger.info("Requesting to refreshToken")
        return objectMapper.readValue(execute(post(form, sourceType)), RestOauth2AccessToken::class.java)
    }


    fun revokeToken(accessToken: String, sourceType: String): Boolean {
        val form = FormBody.Builder().add("token", accessToken).build();
        logger.info("Requesting to revoke access token");

        httpClient.newCall(post(form, sourceType)).execute().use { response ->
            return response.isSuccessful
        }

    }

    private fun post(form: FormBody, sourceType: String): Request {
        val authorizationConfig = configMap[sourceType]
            ?: throw HttpBadRequestException("client-config-not-found", "Cannot find client configurations for source-type $sourceType")

        return Request.Builder().apply {
            url(authorizationConfig.tokenEndpoint)
            post(form)
            header("Authorization", Credentials.basic(authorizationConfig.clientId, authorizationConfig.clientSecret))
            header("Content-Type", "application/x-www-form-urlencoded")
            header("Accept", "application/json")
        }.build()
    }

    private fun execute(request: Request): String {
        return httpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful) {
                response.body?.string()
                    ?: throw HttpBadGatewayException("ManagementPortal did not provide a result")
            } else {
                logger.error("Cannot connect to managementportal ", response.code)
                throw HttpBadGatewayException("Cannot connect to managementportal : Response-code ${response.code}")
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RestSourceAuthorizationService::class.java)
    }
}
