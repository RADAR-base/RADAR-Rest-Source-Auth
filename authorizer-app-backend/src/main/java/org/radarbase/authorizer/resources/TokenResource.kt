package org.radarbase.authorizer.resources

import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.radarbase.authorizer.api.AuthEndpoint
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.StateCreateDTO
import org.radarbase.authorizer.api.Token
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.TokenRepository
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceUserService
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpInternalServerException
import java.net.URI

@Path("tokens")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class TokenResource(
    @Context private val tokenRepository: TokenRepository,
    @Context private val restSourceUserService: RestSourceUserService,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val userRepository: RestSourceUserRepository,
) {
    @POST
    fun createState(
        createState: StateCreateDTO,
    ): Response {

        val secret = if (createState.persistent) {
            Hmac256Secret.generate(secretLength = 12, saltLength = 6)
        } else {
            Hmac256Secret.generate(secretLength = 6, saltLength = 3)
        }
        val user = restSourceUserService.ensureUser(createState.userId)
        val tokenState = tokenRepository.generate(user, secret, createState.persistent) ?: throw HttpInternalServerException("token_not_generated", "Failed to generate token.")

        val token = Token(
            token = tokenState.token,
            secret = secret.secret,
            userId = tokenState.user.id ?: throw HttpInternalServerException("token_incomplete", "Failed to generate complete token."),
            expiresAt = tokenState.expiresAt,
        )
        return Response.created(URI("token/${token.token}"))
            .entity(token)
            .build()
    }

    @GET
    @Path("{token}")
    fun state(
        @PathParam("token") tokenId: String,
    ): Token {
        val tokenState = tokenRepository[tokenId]
            ?: throw HttpBadRequestException("token_not_found", "State has expired or not found")
        if (!tokenState.isValid) throw HttpBadRequestException("token_expired", "Token has expired")
        return Token(
            token = tokenState.token,
            userId = tokenState.user.id!!,
            expiresAt = tokenState.expiresAt,
        )
    }

    @POST
    @Path("{token}/auth-endpoint")
    fun authEndpoint(
        @PathParam("token") tokenId: String,
        @QueryParam("secret") secret: String,
    ): AuthEndpoint {
        val token = tokenRepository[tokenId]
            ?: throw HttpBadRequestException("token_not_found", "State has expired or not found")
        if (!token.isValid) throw HttpBadRequestException("token_expired", "Token has expired")
        val hmac256Secret = Hmac256Secret(secret, token.salt, token.secretHash)
        if (!hmac256Secret.isValid) throw HttpBadRequestException("bad_secret", "Secret does not match token")
        return AuthEndpoint(
            url = authorizationService.getAuthorizationEndpointWithParams(token.user.sourceType, token.user.id!!, token.token),
            state = token.token,
        )
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{token}/authorization")
    fun addAccount(
        @PathParam("type") sourceType: String,
        payload: RequestTokenPayload,
    ): Response {
        RestSourceUserResource.logger.info("Authorizing with payload $payload")

        val stateId = payload.state
        val state = tokenRepository[stateId]
            ?: throw HttpBadRequestException("state_not_found", "State has expired or not found")
        if (!state.isValid) throw HttpBadRequestException("state_expired", "State has expired")

        val accessToken = authorizationService.requestAccessToken(payload, sourceType)
        val user = userRepository.updateToken(accessToken, state.user)

        return Response.created(URI("source-clients/${sourceType}/authorization/${user.externalUserId}"))
            .build()
    }

    @DELETE
    @Path("{token}")
    fun deleteState(
        @PathParam("token") tokenId: String,
    ): Response {
        tokenRepository -= tokenId
        return Response.noContent().build()
    }
}
