package org.radarbase.authorizer.service

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.api.TokenDTO
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.auth.AuthService
import org.radarbase.jersey.exception.HttpApplicationException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.exception.HttpNotFoundException
import kotlin.time.Duration.Companion.seconds

class RestSourceUserService(
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
    @Context private val lockService: LockService,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val authService: AuthService,
) {
    suspend fun ensureUser(userId: Long, permission: Permission? = null): RestSourceUser {
        val user = userRepository.read(userId)
            ?: throw HttpNotFoundException("user_not_found", "Rest-Source-User with ID $userId does not exist")
        if (permission != null) {
            authService.checkPermission(
                permission,
                EntityDetails(
                    project = user.projectId,
                    subject = user.userId,
                ),
            )
        }
        return user
    }

    suspend fun create(userDto: RestSourceUserDTO): RestSourceUserDTO {
        userDto.ensure()

        val existingUser = userRepository.findByUserIdProjectIdSourceType(
            userId = userDto.userId!!,
            projectId = userDto.projectId!!,
            sourceType = userDto.sourceType,
        )
        if (existingUser != null) {
            val response = Response.status(Response.Status.CONFLICT)
                .entity(mapOf("status" to 409, "message" to "User already exists.", "user" to userMapper.fromEntity(existingUser)))
                .build()

            throw WebApplicationException(response)
        }

        val user = userRepository.create(userDto)
        return userMapper.fromEntity(user)
    }

    suspend fun get(userId: Long): RestSourceUserDTO =
        userMapper.fromEntity(ensureUser(userId, Permission.SUBJECT_READ))

    suspend fun delete(userId: Long) {
        ensureUser(userId, Permission.SUBJECT_UPDATE)
        runLocked(userId) { user ->
            if (user.accessToken != null) authorizationService.revokeToken(user)
            userRepository.delete(user)
        }
    }

    suspend fun update(userId: Long, user: RestSourceUserDTO): RestSourceUserDTO {
        user.ensure()

        return userMapper.fromEntity(
            runLocked(userId) {
                userRepository.update(userId, user)
            },
        )
    }

    suspend fun reset(userId: Long, user: RestSourceUserDTO): RestSourceUserDTO {
        user.ensure()
        val existingUser = ensureUser(userId)
        return userMapper.fromEntity(
            userRepository.reset(
                existingUser,
                user.startDate,
                user.endDate ?: existingUser.endDate,
            ),
        )
    }

    private suspend fun RestSourceUserDTO.ensure() {
        val projectId = projectId
            ?: throw HttpBadRequestException("missing_project_id", "project cannot be empty")
        val subjectId = userId
            ?: throw HttpBadRequestException(
                "missing_user_id",
                "subject-id/user-id cannot be empty",
            )

        authService.checkPermission(
            Permission.SUBJECT_UPDATE,
            EntityDetails(
                project = projectId,
                subject = subjectId,
            ),
        )
    }

    /**
     * Validates that an external user ID is not already in use by another user.
     * Should be called before updating a user's token with an external user ID.
     */
    private suspend fun validateExternalUserId(token: RestOauth2AccessToken?, user: RestSourceUser) {
        if (token?.externalUserId != null) {
            val existingUser = userRepository.findByExternalId(token.externalUserId, user.sourceType)
            if (existingUser != null && existingUser.id != user.id) {
                throw HttpConflictException(
                    "external_user_id_already_exists",
                    "External user ID ${token.externalUserId} is already registered for another user of source type ${user.sourceType}",
                )
            }
        }
    }

    /**
     * Updates a user's token after validating the external user ID.
     * This method ensures no duplicate external user IDs exist in the system.
     */
    suspend fun updateUserToken(token: RestOauth2AccessToken?, user: RestSourceUser): RestSourceUser {
        validateExternalUserId(token, user)
        return userRepository.updateToken(token, user)
    }

    suspend fun ensureToken(userId: Long): TokenDTO {
        ensureUser(userId, Permission.MEASUREMENT_CREATE)
        return runLocked(userId) { user ->
            if (user.hasValidToken()) {
                TokenDTO(user.accessToken, user.expiresAt)
            } else {
                // refresh token if current token is already expired.
                doRefreshToken(user)
            }
        }
    }

    suspend fun refreshToken(userId: Long): TokenDTO {
        ensureUser(userId, Permission.MEASUREMENT_CREATE)
        return runLocked(userId) { user ->
            doRefreshToken(user)
        }
    }

    private suspend inline fun <T> runLocked(
        userId: Long,
        crossinline doRun: suspend (RestSourceUser) -> T,
    ): T =
        lockService.runLocked("token-$userId", 10.seconds) {
            val user = ensureUser(userId)
            doRun(user)
        }

    private suspend fun doRefreshToken(user: RestSourceUser): TokenDTO {
        if (!user.authorized) {
            throw HttpApplicationException(
                Response.Status.PROXY_AUTHENTICATION_REQUIRED,
                "user_unauthorized",
                "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid.",
            )
        }
        if (user.refreshToken == null) {
            throw HttpApplicationException(
                Response.Status.PROXY_AUTHENTICATION_REQUIRED,
                "user_unauthorized",
                "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid.",
            )
        }

        val token = authorizationService.refreshToken(user)
        val updatedUser = updateUserToken(token, user)

        if (!updatedUser.authorized) {
            throw HttpApplicationException(
                Response.Status.PROXY_AUTHENTICATION_REQUIRED,
                "user_unauthorized",
                "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid. Invalidated user authorization.",
            )
        }
        return TokenDTO(updatedUser.accessToken, updatedUser.expiresAt)
    }
}
