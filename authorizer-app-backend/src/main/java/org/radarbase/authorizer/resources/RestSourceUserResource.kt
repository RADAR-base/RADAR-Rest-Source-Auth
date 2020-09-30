/*
 *  Copyright 2020 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.authorizer.resources

import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.util.StateStore
import org.radarbase.authorizer.util.StateStore.State.Companion.toState
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.exception.HttpApplicationException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.radarcns.auth.authorization.Permission
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
@Singleton
class RestSourceUserResource(
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val projectService: RadarProjectService,
    @Context private val stateStore: StateStore,
    @Context private val auth: Auth
) {

    @GET
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.READ)
    fun query(
        @QueryParam("project-id") projectId: String?,
        @QueryParam("source-type") sourceType: String?,
        @QueryParam("size") pageSize: Int?,
        @DefaultValue("1") @QueryParam("page") pageNumber: Int): RestSourceUsers {

        val projects = if (projectId != null) {
            auth.checkPermissionOnProject(Permission.SUBJECT_READ, projectId)
            listOf(projectId)
        } else {
            projectService.userProjects(auth, Permission.SUBJECT_READ)
                    .map { it.id }
        }

        if (projects.isEmpty()) return RestSourceUsers(emptyList())

        val queryPage = Page(pageNumber = pageNumber, pageSize = pageSize)
        val (records, page) = userRepository.query(queryPage, projects, sourceType)

        return userMapper.fromRestSourceUsers(records.filter {
            auth.token.hasPermissionOnSubject(Permission.SUBJECT_READ, it.projectId, it.userId)
        }, page)
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    fun create(
        @FormParam("code") code: String,
        @FormParam("state") reqState: String): Response {
        logger.info("Authorizing with code $code state $reqState")
        val state = reqState.toState()
        if (!stateStore.isValid(state)) throw HttpBadRequestException("state_not_found", "State has expired or not found")
        val accessToken = authorizationService.requestAccessToken(code, sourceType = state.sourceType)
        val user = userRepository.create(accessToken, state.sourceType)

        return Response.created(URI("users/${user.id}"))
            .entity(userMapper.fromEntity(user))
            .build()
    }

    @POST
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun update(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO): RestSourceUserDTO {
        val existingUser = validate(userId, user, Permission.SUBJECT_UPDATE)

        val updatedUser = userRepository.update(existingUser, user)
        return userMapper.fromEntity(updatedUser)
    }

    @GET
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.READ)
    fun readUser(@PathParam("id") userId: Long): RestSourceUserDTO {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.SUBJECT_READ, user.projectId, user.userId)
        return userMapper.fromEntity(user)
    }

    @DELETE
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun deleteUser(@PathParam("id") userId: Long): Response {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, user.projectId, user.userId)
        if (user.accessToken != null) {
            authorizationService.revokeToken(user.accessToken!!, user.sourceType)
        }
        userRepository.delete(user)
        return Response.noContent().header("user-removed", userId).build()
    }

    @POST
    @Path("{id}/reset")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun reset(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO): RestSourceUserDTO {
        val existingUser = validate(userId, user, Permission.SUBJECT_UPDATE)

        val updatedUser = userRepository.reset(existingUser, user.startDate, user.endDate
            ?: existingUser.endDate)
        return userMapper.fromEntity(updatedUser)
    }

    @GET
    @Path("{id}/token")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun requestToken(@PathParam("id") userId: Long): TokenDTO {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.MEASUREMENT_CREATE, user.projectId, user.userId)
        if (!user.authorized) {
            throw HttpApplicationException(Response.Status.PROXY_AUTHENTICATION_REQUIRED, "user_unauthorized", "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid.")
        }
        return TokenDTO(user.accessToken, user.expiresAt)
    }

    @POST
    @Path("{id}/token")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun refreshToken(@PathParam("id") userId: Long): TokenDTO {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.MEASUREMENT_CREATE, user.projectId, user.userId)
        if (!user.authorized) {
            throw HttpApplicationException(Response.Status.PROXY_AUTHENTICATION_REQUIRED, "user_unauthorized", "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid.")
        }
        val rft = user.refreshToken
                ?: throw HttpApplicationException(Response.Status.PROXY_AUTHENTICATION_REQUIRED, "user_unauthorized", "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid.")

        val token = authorizationService.refreshToken(rft, user.sourceType)
        val updatedUser = userRepository.updateToken(token, userId)

        if (!updatedUser.authorized) {
            throw HttpApplicationException(Response.Status.PROXY_AUTHENTICATION_REQUIRED, "user_unauthorized", "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid. Invalidated user authorization.")
        }
        return TokenDTO(updatedUser.accessToken, updatedUser.expiresAt)
    }

    private fun validate(id: Long, user: RestSourceUserDTO, permission: Permission): RestSourceUser {
        val existingUser = ensureUser(id)
        val projectId = user.projectId
            ?: throw HttpBadRequestException("missing_project_id", "project cannot be empty")
        val userId = user.userId
            ?: throw HttpBadRequestException("missing_user_id", "subject-id/user-id cannot be empty")
        auth.checkPermissionOnSubject(permission, projectId, userId)

        projectService.projectUsers(projectId).find { it.id == userId }
            ?: throw HttpBadRequestException("user_not_found", "user $userId not found in project $projectId")
        return existingUser
    }

    private fun ensureUser(userId: Long): RestSourceUser {
        return userRepository.read(userId)
            ?: throw HttpNotFoundException("user_not_found", "Rest-Source-User with ID $userId does not exist")
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RestSourceUserResource::class.java)
    }
}
