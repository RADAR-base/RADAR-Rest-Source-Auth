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
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceClientService
import org.radarbase.authorizer.service.RestSourceUserService
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.service.managementportal.RadarProjectService
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
    @Context private val auth: Auth,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val sourceClientService: RestSourceClientService,
    @Context private val userService: RestSourceUserService,
) {
    @GET
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.READ)
    fun query(
        @QueryParam("project-id") projectId: String?,
        @QueryParam("source-type") sourceType: String?,
        @QueryParam("search") search: String?,
        @DefaultValue("true") @QueryParam("authorized") isAuthorized: Boolean?,
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
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.CREATE)
    fun create(
        userDto: RestSourceUserDTO,
    ): Response {
        val user = userService.create(userDto)

        return Response.created(URI("users/${user.id}"))
            .entity(user)
            .build()
    }

    @POST
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun update(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO,
    ): RestSourceUserDTO = userService.update(userId, user)

    @GET
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.READ)
    @Cache(maxAge = 300, isPrivate = true)
    fun readUser(@PathParam("id") userId: Long): RestSourceUserDTO = userService.get(userId)

    @DELETE
    @Path("{id}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun deleteUser(@PathParam("id") userId: Long): Response {
        userService.delete(userId)
        return Response.noContent().header("user-removed", userId).build()
    }

    @POST
    @Path("{id}/reset")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun reset(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO,
    ): RestSourceUserDTO = userService.reset(userId, user)

    @GET
    @Path("{id}/token")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun requestToken(@PathParam("id") userId: Long): TokenDTO = userService.ensureToken(userId)

    @POST
    @Path("{id}/token")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.CREATE)
    fun refreshToken(@PathParam("id") userId: Long): TokenDTO = userService.refreshToken(userId)

    @POST
    @Path("{id}/token/sign")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.READ)
    fun signRequest(
        @PathParam("id") userId: Long,
        payload: SignRequestParams,
    ): SignRequestParams {
        val user = userService.ensureUser(userId, Permission.MEASUREMENT_READ)
        return authorizationService.signRequest(user, payload)
    }

    companion object {
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
