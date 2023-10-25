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
import jakarta.ws.rs.DELETE
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
import org.radarbase.auth.authorization.EntityDetails
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.DeregistrationsDTO
import org.radarbase.authorizer.api.RestSourceClientMapper
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.api.ShareableClientDetail
import org.radarbase.authorizer.api.ShareableClientDetails
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceClientService
import org.radarbase.jersey.auth.AuthService
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.kotlin.coroutines.forkJoin
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Path("source-clients")
@Produces(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class SourceClientResource(
    @Context private val restSourceClients: RestSourceClientService,
    @Context private val clientMapper: RestSourceClientMapper,
    @Context private val authService: AuthService,
    @Context private val asyncService: AsyncCoroutineService,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
) {
    private val sharableClientDetails = clientMapper.fromSourceClientConfigs(restSourceClients.clients)

    @GET
    @Authenticated
    @NeedsPermission(Permission.SOURCETYPE_READ)
    @Cache(maxAge = 3600, isPrivate = true, vary = [HttpHeaders.AUTHORIZATION])
    fun clients(): ShareableClientDetails = sharableClientDetails

    @GET
    @Authenticated
    @Path("{type}")
    @NeedsPermission(Permission.SOURCETYPE_READ)
    fun client(@PathParam("type") type: String): ShareableClientDetail {
        val sourceType = restSourceClients.forSourceType(type)
        return clientMapper.toSourceClientConfig(sourceType)
    }

    @DELETE
    @Authenticated
    @Path("{type}/authorization/{serviceUserId}")
    @NeedsPermission(Permission.SUBJECT_UPDATE)
    fun deleteAuthorizationWithToken(
        @PathParam("serviceUserId") serviceUserId: String,
        @PathParam("type") sourceType: String,
        @QueryParam("accessToken") accessToken: String?,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        restSourceClients.ensureSourceType(sourceType)
        val user = userRepository.findByExternalId(serviceUserId, sourceType)
        if (user == null) {
            if (accessToken.isNullOrEmpty()) throw HttpNotFoundException("user-not-found", "User and access token not valid")
            logger.info("No user found for external ID provided. Continuing deregistration..")
            authorizationService.revokeToken(serviceUserId, sourceType, accessToken)
        } else {
            authService.checkPermission(
                Permission.SUBJECT_UPDATE,
                EntityDetails(
                    project = user.projectId,
                    subject = user.userId,
                ),
            )
            authorizationService.revokeToken(user)
        }
    }

    @GET
    @Authenticated
    @Path("{type}/authorization/{serviceUserId}")
    @NeedsPermission(Permission.MEASUREMENT_READ)
    fun getUserByServiceUserId(
        @PathParam("serviceUserId") serviceUserId: String,
        @PathParam("type") sourceType: String,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        restSourceClients.ensureSourceType(sourceType)
        val user = userRepository.findByExternalId(serviceUserId, sourceType)
            ?: throw HttpNotFoundException("user-not-found", "User with service user id not found.")

        authService.withPermission(
            Permission.MEASUREMENT_READ,
            EntityDetails(
                project = user.projectId,
                subject = user.userId,
            ),
        ) {
            userMapper.fromEntity(user)
        }
    }

    /**
     * Deletes a user from the user repository for a particular source-type.
     * This should be called when a trigger from the external service is received to deregister a user
     * (for example if a user revokes permissions to collect data in the external app).
     * This endpoint is not authenticated but supports validation of the external userId and user access token
     * received in the request.
     *
     * @param sourceType the type of source. Currently, only supports "Garmin" source type.
     * @property body contains user information to be removed. The body should contain an external userId
     * and the user access token so the user can be validated.
     */
    @POST
    @Path("{type}/deregister")
    fun reportDeregistration(
        @PathParam("type") sourceType: String,
        body: DeregistrationsDTO,
        @Suspended asyncResponse: AsyncResponse,
    ) = asyncService.runAsCoroutine(asyncResponse) {
        restSourceClients.ensureSourceType(sourceType)

        body.deregistrations.forkJoin { deregistration ->
            val user = userRepository.findByExternalId(deregistration.userId, sourceType)
            if (user != null && user.accessToken == deregistration.userAccessToken) {
                authorizationService.deregisterUser(user)
            }
        }
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SourceClientResource::class.java)
    }
}
