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
import jakarta.ws.rs.Consumes
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.DefaultValue
import jakarta.ws.rs.GET
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.container.AsyncResponse
import jakarta.ws.rs.container.Suspended
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.HttpHeaders
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.api.RestSourceUsers
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceClientService
import org.radarbase.authorizer.service.RestSourceUserService
import org.radarbase.jersey.auth.AuthService
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.service.AsyncCoroutineService
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
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val sourceClientService: RestSourceClientService,
    @Context private val userService: RestSourceUserService,
    @Context private val asyncService: AsyncCoroutineService,
    @Context private val authService: AuthService,
) {
    @GET
    @NeedsPermission(Permission.SUBJECT_READ)
    fun query(
        @QueryParam("project-id") projectId: String?,
        @QueryParam("source-type") sourceType: String?,
        @QueryParam("search") search: String?,
        @DefaultValue("true")
        @QueryParam("authorized")
        isAuthorized: String,
        @DefaultValue(Integer.MAX_VALUE.toString())
        @QueryParam("size")
        pageSize: Int,
        @DefaultValue("1")
        @QueryParam("page")
        pageNumber: Int,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val projectIds = if (projectId == null) {
            projectService.userProjects(Permission.SUBJECT_READ)
                .also { projects -> if (projects.isEmpty()) return@runAsCoroutine emptyUsers(pageNumber, pageSize) }
                .map { it.id }
        } else {
            authService.checkPermission(Permission.SUBJECT_READ, EntityDetails(project = projectId))
            listOf(projectId)
        }

        val sanitizedSourceType = when (sourceType) {
            null -> null
            in sourceClientService -> sourceType
            else -> return@runAsCoroutine emptyUsers(pageNumber, pageSize)
        }

        val sanitizedSearch = search?.takeIf { it.length >= 2 }

        val userIds = if (sanitizedSearch != null) {
            projectId ?: throw HttpBadRequestException(
                "missing_project_id",
                "Cannot search without a fixed project ID.",
            )
            projectService.projectSubjects(projectId)
                .mapNotNull { sub ->
                    val externalId = sub.externalId ?: return@mapNotNull null
                    sub.id.takeIf { sanitizedSearch in externalId }
                }
        } else {
            emptyList()
        }

        val authorizedBoolean = when (isAuthorized) {
            "true", "yes" -> true
            "false", "no" -> false
            else -> null
        }

        val queryPage = Page(pageNumber = pageNumber, pageSize = pageSize)
        val (records, page) = userRepository.query(
            queryPage,
            projectIds,
            sanitizedSourceType,
            sanitizedSearch,
            userIds,
            authorizedBoolean,
        )

        userMapper.fromRestSourceUsers(records, page)
    }

    @POST
    @NeedsPermission(Permission.SUBJECT_CREATE)
    fun create(
        userDto: RestSourceUserDTO,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val user = userService.create(userDto)

        Response.created(URI("users/${user.id}"))
            .entity(user)
            .build()
    }

    @POST
    @Path("{id}")
    @NeedsPermission(Permission.SUBJECT_UPDATE)
    fun update(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        userService.update(userId, user)
    }

    @GET
    @Path("{id}")
    @NeedsPermission(Permission.SUBJECT_READ)
    @Cache(maxAge = 300, isPrivate = true, vary = [HttpHeaders.AUTHORIZATION])
    fun readUser(
        @PathParam("id") userId: Long,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        userService.get(userId)
    }

    @DELETE
    @Path("{id}")
    @NeedsPermission(Permission.SUBJECT_UPDATE)
    fun deleteUser(
        @PathParam("id") userId: Long,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        userService.delete(userId)
        Response.noContent()
            .header("user-removed", userId)
            .build()
    }

    @POST
    @Path("{id}/reset")
    @NeedsPermission(Permission.SUBJECT_UPDATE)
    fun reset(
        @PathParam("id") userId: Long,
        user: RestSourceUserDTO,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        userService.reset(userId, user)
    }

    @GET
    @Path("{id}/token")
    @NeedsPermission(Permission.MEASUREMENT_CREATE)
    fun requestToken(
        @PathParam("id") userId: Long,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        userService.ensureToken(userId)
    }

    @POST
    @Path("{id}/token")
    @NeedsPermission(Permission.MEASUREMENT_CREATE)
    fun refreshToken(
        @PathParam("id") userId: Long,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        userService.refreshToken(userId)
    }

    @POST
    @Path("{id}/token/sign")
    @NeedsPermission(Permission.MEASUREMENT_READ)
    fun signRequest(
        @PathParam("id") userId: Long,
        payload: SignRequestParams,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        val user = userService.ensureUser(userId, Permission.MEASUREMENT_READ)
        authorizationService.signRequest(user, payload)
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
