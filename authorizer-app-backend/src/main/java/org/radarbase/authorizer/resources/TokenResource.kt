package org.radarbase.authorizer.resources

import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.TokenRepository
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceUserService
import org.radarbase.authorizer.service.TokenService
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.exception.HttpBadRequestException
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
    @Context private val tokenService: TokenService,
) {
    @POST
    @Authenticated
    @NeedsPermission(entity = Permission.Entity.SUBJECT, operation = Permission.Operation.UPDATE)
    fun createState(
        @Context auth: Auth,
        createState: StateCreateDTO,
    ): Response {
        val user = restSourceUserService.ensureUser(createState.userId.toLong())
        auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, user.projectId, user.userId)
        var tokenState = tokenService.generate(user, createState.persistent)
        if (!createState.persistent) {
            tokenState = tokenState.copy(
                authEndpointUrl = authorizationService.getAuthorizationEndpointWithParams(user.sourceType,
                    user.id!!,
                    tokenState.token),
            )
        }
        return Response.created(URI("tokens/${tokenState.token}"))
            .entity(tokenState)
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
            userId = tokenState.user.id!!.toString(),
            expiresAt = tokenState.expiresAt,
            persistent = tokenState.persistent,
        )
    }

    @DELETE
    @Path("{token}")
    fun deleteState(
        @PathParam("token") token: String,
    ): Response {
        tokenRepository -= token
        return Response.noContent().build()
    }

    @POST
    @Path("{token}")
    fun authEndpoint(
        @PathParam("token") token: String,
        tokenSecret: TokenSecret,
    ): Token {
        val tokenState = tokenRepository[token]
            ?: throw HttpBadRequestException("token_not_found", "State has expired or not found")
        val salt = tokenState.salt
        val secretHash = tokenState.secretHash
        if (salt == null || secretHash == null) throw HttpBadRequestException("token_invalid", "Cannot retrieve authentication endpoint token without credentials.")
        if (!tokenState.isValid) throw HttpBadRequestException("token_expired", "Token has expired")
        val hmac256Secret = Hmac256Secret(tokenSecret.secret, salt, secretHash)
        if (!hmac256Secret.isValid) throw HttpBadRequestException("bad_secret", "Secret does not match token")
        return Token(
            token = tokenState.token,
            authEndpointUrl = authorizationService.getAuthorizationEndpointWithParams(tokenState.user.sourceType, tokenState.user.id!!, tokenState.token),
            userId = tokenState.user.id!!.toString(),
            expiresAt = tokenState.expiresAt,
            persistent = tokenState.persistent,
        )
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{token}/authorize")
    fun addAccount(
        @PathParam("token") token: String,
        payload: RequestTokenPayload,
    ): Response {
        val tokenState = tokenRepository[token]
            ?: throw HttpBadRequestException("state_not_found", "State has expired or not found")
        if (!tokenState.isValid) throw HttpBadRequestException("state_expired", "State has expired")

        val accessToken = authorizationService.requestAccessToken(payload, tokenState.user.sourceType)
        val user = userRepository.updateToken(accessToken, tokenState.user)

        val tokenEntity = Token(
            token = tokenState.token,
            userId = tokenState.user.id!!.toString(),
            expiresAt = tokenState.expiresAt,
            persistent = tokenState.persistent,
        )

        tokenRepository -= tokenState

        return Response.created(URI("source-clients/${user.sourceType}/authorization/${user.externalUserId}"))
            .entity(tokenEntity)
            .build()
    }
}
