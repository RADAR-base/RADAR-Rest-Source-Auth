package org.radarbase.authorizer.resources

import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.service.RegistrationService
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceUserService
import org.radarbase.authorizer.util.Hmac256Secret
import org.radarbase.jersey.auth.AuthService
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.jersey.service.managementportal.RadarProjectService
import java.net.URI

@Path("registrations")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class RegistrationResource(
    @Context private val registrationRepository: RegistrationRepository,
    @Context private val restSourceUserService: RestSourceUserService,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val registrationService: RegistrationService,
    @Context private val projectService: RadarProjectService,
    @Context private val authService: AuthService,
    @Context private val asyncService: AsyncCoroutineService,
) {
    @POST
    @Authenticated
    @NeedsPermission(Permission.SUBJECT_UPDATE)
    fun createState(
        createState: StateCreateDTO,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val user = restSourceUserService.ensureUser(createState.userId.toLong())
        authService.checkPermission(
            Permission.SUBJECT_UPDATE,
            EntityDetails(
                project = user.projectId,
                subject = user.userId,
            ),
        )
        var tokenState = registrationService.generate(user, createState.persistent)
        if (!createState.persistent) {
            tokenState = tokenState.copy(
                authEndpointUrl = authorizationService.getAuthorizationEndpointWithParams(
                    sourceType = user.sourceType,
                    userId = user.id!!,
                    state = tokenState.token,
                ),
            )
        }
        Response.created(URI("tokens/${tokenState.token}"))
            .entity(tokenState)
            .build()
    }

    @GET
    @Path("{token}")
    fun state(
        @PathParam("token") token: String,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val registration = registrationService.ensureRegistration(token)
        RegistrationResponse(
            token = registration.token,
            userId = registration.user.id!!.toString(),
            createdAt = registration.createdAt,
            expiresAt = registration.expiresAt,
            persistent = registration.persistent,
            sourceType = registration.user.sourceType,
        )
    }

    @DELETE
    @Authenticated
    @NeedsPermission(Permission.SUBJECT_UPDATE)
    @Path("{token}")
    fun deleteState(
        @PathParam("token") token: String,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        registrationRepository.remove(token)
        Response.noContent().build()
    }

    @POST
    @Path("{token}")
    fun authEndpoint(
        @PathParam("token") token: String,
        tokenSecret: TokenSecret,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val registration = registrationService.ensureRegistration(token)
        if (registration.user.authorized) throw HttpConflictException("user_already_authorized", "User was already authorized for this service.")
        val salt = registration.salt
        val secretHash = registration.secretHash
        if (salt == null || secretHash == null) throw HttpBadRequestException("registration_invalid", "Cannot retrieve authentication endpoint token without credentials.")
        val hmac256Secret = Hmac256Secret(tokenSecret.secret, salt, secretHash)
        if (!hmac256Secret.isValid) throw HttpBadRequestException("bad_secret", "Secret does not match token")
        val project = registration.user.projectId?.let {
            projectService.project(it).toProject()
        }

        RegistrationResponse(
            token = registration.token,
            authEndpointUrl = authorizationService.getAuthorizationEndpointWithParams(
                sourceType = registration.user.sourceType,
                userId = registration.user.id!!,
                state = registration.token,
            ),
            userId = registration.user.id!!.toString(),
            project = project,
            createdAt = registration.createdAt,
            expiresAt = registration.expiresAt,
            persistent = registration.persistent,
            sourceType = registration.user.sourceType,
        )
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{token}/authorize")
    fun addAccount(
        @PathParam("token") token: String,
        payload: RequestTokenPayload,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val registration = registrationService.ensureRegistration(token)
        val accessToken = authorizationService.requestAccessToken(payload, registration.user.sourceType)
        val user = userRepository.updateToken(accessToken, registration.user)
        val project = registration.user.projectId?.let {
            projectService.project(it).toProject()
        }

        val tokenEntity = RegistrationResponse(
            token = registration.token,
            userId = registration.user.id!!.toString(),
            project = project,
            createdAt = registration.createdAt,
            expiresAt = registration.expiresAt,
            persistent = registration.persistent,
            sourceType = registration.user.sourceType,
        )

        registrationRepository.remove(registration)

        Response.created(URI("source-clients/${user.sourceType}/authorization/${user.externalUserId}"))
            .entity(tokenEntity)
            .build()
    }
}
