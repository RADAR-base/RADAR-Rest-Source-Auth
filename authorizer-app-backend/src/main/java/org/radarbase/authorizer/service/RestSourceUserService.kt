package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.Response
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.api.TokenDTO
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.exception.HttpApplicationException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.managementportal.RadarProjectService
import java.time.Duration

class RestSourceUserService(
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
    @Context private val lockService: LockService,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val projectService: RadarProjectService,
    @Context private val auth: Auth,
) {
    fun ensureUser(userId: Long, permission: Permission? = null): RestSourceUser {
        val user = userRepository.read(userId)
            ?: throw HttpNotFoundException("user_not_found", "Rest-Source-User with ID $userId does not exist")
        if (permission != null) {
            auth.checkPermissionOnSubject(permission, user.projectId, user.userId)
        }
        return user
    }

    fun create(userDto: RestSourceUserDTO): RestSourceUserDTO {
        validate(userDto)
        val user = userRepository.create(userDto)
        return userMapper.fromEntity(user)
    }

    fun get(userId: Long): RestSourceUserDTO {
        return userMapper.fromEntity(
            ensureUser(userId, Permission.SUBJECT_READ)
        )
    }

    fun delete(userId: Long) {
        ensureUser(userId, Permission.SUBJECT_UPDATE)
        runLocked(userId) { user ->
            if (user.accessToken != null) authorizationService.revokeToken(user)
            userRepository.delete(user)
        }
    }

    fun update(userId: Long, user: RestSourceUserDTO): RestSourceUserDTO {
        validate(user)
        return userMapper.fromEntity(
            runLocked(userId) {
                userRepository.update(userId, user)
            }
        )
    }

    fun reset(userId: Long, user: RestSourceUserDTO): RestSourceUserDTO {
        validate(user)
        val existingUser = ensureUser(userId)
        return userMapper.fromEntity(
            userRepository.reset(
                existingUser,
                user.startDate,
                user.endDate ?: existingUser.endDate,
            )
        )
    }

    private fun validate(
        user: RestSourceUserDTO,
    ) {
        val projectId = user.projectId
            ?: throw HttpBadRequestException("missing_project_id", "project cannot be empty")
        val userId = user.userId
            ?: throw HttpBadRequestException(
                "missing_user_id", "subject-id/user-id cannot be empty",
            )
        auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, projectId, userId)

        projectService.projectUsers(projectId).find { it.id == userId }
            ?: throw HttpBadRequestException(
                "user_not_found", "user $userId not found in project $projectId"
            )
    }

    fun ensureToken(userId: Long): TokenDTO {
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

    fun refreshToken(userId: Long): TokenDTO {
        ensureUser(userId, Permission.MEASUREMENT_CREATE)
        return runLocked(userId) { user ->
            doRefreshToken(user)
        }
    }

    private inline fun <T> runLocked(userId: Long, crossinline doRun: (RestSourceUser) -> T): T {
        return lockService.runLocked("token-$userId", Duration.ofSeconds(10)) {
            doRun(ensureUser(userId))
        }
    }

    private fun doRefreshToken(user: RestSourceUser): TokenDTO {
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
        val updatedUser = userRepository.updateToken(token, user)

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
