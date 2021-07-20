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

import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceClientService
import org.radarbase.authorizer.util.StateStore
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.exception.HttpApplicationException
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.net.URI

@Path("users")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
@Singleton
class RestSourceUserResource(
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
    @Context private val projectService: RadarProjectService,
    @Context private val stateStore: StateStore,
    @Context private val auth: Auth,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val sourceClientService: RestSourceClientService
) {
    @GET
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.READ)
    fun query(
        @QueryParam("project-id") projectId: String?,
        @QueryParam("source-type") sourceType: String?,
        @QueryParam("search") search: String?,
        @QueryParam("authorized") isAuthorized: Boolean?,
        @DefaultValue(Integer.MAX_VALUE.toString()) @QueryParam("size") pageSize: Int,
        @DefaultValue("1") @QueryParam("page") pageNumber: Int,
    ): RestSourceUsers {
        val projectIds = if (projectId == null) {
            projectService.userProjects(auth, Permission.SUBJECT_READ)
                .also { projects ->
                    if (projects.isEmpty()) return emptyUsers(pageNumber, pageSize)
                }
                .map { it.id }
        } else {
            auth.checkPermissionOnProject(Permission.SUBJECT_READ, projectId)
            listOf(projectId)
        }

        val sanitizedSourceType = when (sourceType) {
            null -> null
            in sourceClientService -> sourceType
            else -> return emptyUsers(pageNumber, pageSize)
        }

        val sanitizedSearch = search?.takeIf { it.length >= 2 }

        val userIds = if (sanitizedSearch != null) {
            if (projectId == null) {
                throw HttpBadRequestException(
                    "missing_project_id",
                    "Cannot search without a fixed project ID.",
                )
            }
            projectService.projectUsers(projectId)
                .mapNotNull { sub ->
                    val externalId = sub.externalId ?: return@mapNotNull null
                    sub.id.takeIf { sanitizedSearch in externalId }
                }
        } else emptyList()

        val queryPage = Page(pageNumber = pageNumber, pageSize = pageSize)
        val (records, page) = userRepository.query(
            queryPage,
            projectIds,
            sanitizedSourceType,
            sanitizedSearch,
            userIds,
            isAuthorized,
        )

        return userMapper.fromRestSourceUsers(records, page)
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    fun create(
        payload: RequestTokenPayload,
    ): Response {
        logger.info("Authorizing with payload $payload")
        val stateId = payload.state
        if (stateId != null) {
            val state = stateStore[stateId]
                ?: throw HttpBadRequestException("state_not_found", "State has expired or not found")
            if (!state.isValid) throw HttpBadRequestException("state_expired", "State has expired")
        }
        val sourceType = payload.sourceType
        sourceClientService.ensureSourceType(sourceType)

        val accessToken = authorizationService.requestAccessToken(payload, sourceType)
        val user = userRepository.create(accessToken, sourceType)

        return Response.created(URI("users/${user.id}"))
            .entity(userMapper.fromEntity(user))
            .build()
    }

    @POST
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun update(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO,
    ): RestSourceUserDTO {
        val existingUser = validate(userId, user, Permission.SUBJECT_UPDATE)

        val updatedUser = userRepository.update(existingUser, user)
        return userMapper.fromEntity(updatedUser)
    }

    @GET
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.READ)
    @Cache(maxAge = 300, isPrivate = true)
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
        if (user.accessToken != null) authorizationService.revokeToken(user)
        userRepository.delete(user)

        return Response.noContent().header("user-removed", userId).build()
    }

    @POST
    @Path("{id}/reset")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun reset(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO,
    ): RestSourceUserDTO {
        val existingUser = validate(userId, user, Permission.SUBJECT_UPDATE)

        val updatedUser = userRepository.reset(
            existingUser,
            user.startDate,
            user.endDate ?: existingUser.endDate,
        )
        return userMapper.fromEntity(updatedUser)
    }

    @GET
    @Path("{id}/token")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun requestToken(@PathParam("id") userId: Long): TokenDTO {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.MEASUREMENT_CREATE, user.projectId, user.userId)
        return if (user.hasValidToken()) {
            TokenDTO(user.accessToken, user.expiresAt)
        } else {
            // refresh token if current token is already expired.
            refreshToken(userId, user)
        }
    }

    @POST
    @Path("{id}/token")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun refreshToken(@PathParam("id") userId: Long): TokenDTO {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.MEASUREMENT_CREATE, user.projectId, user.userId)
        return refreshToken(userId, user)
    }

    @POST
    @Path("{id}/token/sign")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.READ)
    fun signRequest(
        @PathParam("id") userId: Long,
        payload: SignRequestParams,
    ): SignRequestParams {
        val user = ensureUser(userId)
        auth.checkPermissionOnSubject(Permission.MEASUREMENT_READ, user.projectId, user.userId)

        return authorizationService.signRequest(user, payload)
    }

    private fun refreshToken(userId: Long, user: RestSourceUser): TokenDTO {
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
        val updatedUser = userRepository.updateToken(token, userId)

        if (!updatedUser.authorized) {
            throw HttpApplicationException(
                Response.Status.PROXY_AUTHENTICATION_REQUIRED,
                "user_unauthorized",
                "Refresh token for ${user.userId ?: user.externalUserId} is no longer valid. Invalidated user authorization.",
            )
        }
        return TokenDTO(updatedUser.accessToken, updatedUser.expiresAt)
    }

    private fun validate(
        id: Long,
        user: RestSourceUserDTO,
        permission: Permission,
    ): RestSourceUser {
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

        private fun emptyUsers(pageNumber: Int, pageSize: Int) = RestSourceUsers(
            users = listOf(),
            metadata = Page(
                pageNumber = pageNumber,
                pageSize = pageSize,
                totalElements = 0,
            ),
        )
    }
}
