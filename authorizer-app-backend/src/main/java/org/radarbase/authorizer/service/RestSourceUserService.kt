package org.radarbase.authorizer.service

import jakarta.inject.Provider
import jakarta.ws.rs.core.Context
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.managementportal.RadarProjectService

class RestSourceUserService(
    @Context private val auth: Provider<Auth>,
    @Context private val projectService: RadarProjectService,
    @Context private val userRepository: RestSourceUserRepository,
) {
    fun validate(
        user: RestSourceUserDTO,
        permission: Permission,
    ) {
        val projectId = user.projectId
            ?: throw HttpBadRequestException("missing_project_id", "project cannot be empty")
        val userId = user.userId
            ?: throw HttpBadRequestException("missing_user_id", "subject-id/user-id cannot be empty")
        auth.get().checkPermissionOnSubject(permission, projectId, userId)

        projectService.projectUsers(projectId).find { it.id == userId }
            ?: throw HttpBadRequestException("user_not_found", "user $userId not found in project $projectId")
    }

    fun ensureUser(userId: Long): RestSourceUser {
        return userRepository.read(userId)
            ?: throw HttpNotFoundException("user_not_found", "Rest-Source-User with ID $userId does not exist")
    }
}
