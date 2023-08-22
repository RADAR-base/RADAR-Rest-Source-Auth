package org.radarbase.authorizer.service

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.UriBuilder
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
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

    //TODO: implement this
    override fun deregisterUser(user: RestSourceUser) =
        throw HttpBadRequestException("", "Not available for auth type")

}