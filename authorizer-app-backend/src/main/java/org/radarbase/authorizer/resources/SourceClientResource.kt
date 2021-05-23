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
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceClientService
import org.radarbase.authorizer.util.StateStore
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.exception.HttpNotFoundException
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@Path("source-clients")
@Produces(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class SourceClientResource(
    @Context private val restSourceClients: RestSourceClientService,
    @Context private val clientMapper: RestSourceClientMapper,
    @Context private val stateStore: StateStore,
    @Context private val auth: Auth,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val userMapper: RestSourceUserMapper,
) {
    private val sourceTypes = restSourceClients.clients.map { it.sourceType }
    private val sharableClientDetails = clientMapper.fromSourceClientConfigs(restSourceClients.clients)

    @GET
    @Authenticated
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    @Cache(maxAge = 3600, isPrivate = true)
    fun clients(): ShareableClientDetails = sharableClientDetails

    @GET
    @Authenticated
    @Path("type")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    @Cache(maxAge = 3600, isPrivate = true)
    fun types(): List<String> = sourceTypes

    @GET
    @Authenticated
    @Path("{type}")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun client(@PathParam("type") type: String): ShareableClientDetail {
        val sourceType = restSourceClients.forSourceType(type)
        return clientMapper.toSourceClientConfig(sourceType, stateStore.generate(type).stateId)
    }

    @GET
    @Authenticated
    @Path("{type}/auth-endpoint")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun getAuthEndpoint(
        @PathParam("type") type: String,
        @QueryParam("callbackUrl") callbackUrl: String,
    ): String = authorizationService.getAuthorizationEndpointWithParams(type, callbackUrl)

    @DELETE
    @Authenticated
    @Path("{type}/authorization/{serviceUserId}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun deleteAuthorizationWithToken(
        @PathParam("serviceUserId") serviceUserId: String,
        @PathParam("type") sourceType: String,
        @QueryParam("accessToken") accessToken: String?,
    ): Boolean {
        val user = userRepository.findByExternalId(serviceUserId, sourceType)
        return if (user == null) {
            if (accessToken.isNullOrEmpty()) throw HttpNotFoundException("user-not-found", "User and access token not valid")
            logger.info("No user found for external ID provided. Continuing deregistration..")
            authorizationService.revokeToken(serviceUserId, sourceType, accessToken)
        } else {
            auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, user.projectId, user.userId)
            authorizationService.revokeToken(user)
        }
    }

    @GET
    @Authenticated
    @Path("{type}/authorization/{serviceUserId}")
    @NeedsPermission(Permission.Entity.MEASUREMENT, Permission.Operation.READ)
    fun getUserByServiceUserId(
        @PathParam("serviceUserId") serviceUserId: String,
        @PathParam("type") sourceType: String,
    ): RestSourceUserDTO {
        val user = userRepository.findByExternalId(serviceUserId, sourceType)
            ?: throw HttpNotFoundException("user-not-found", "User with service user id not found.")

        auth.checkPermissionOnSubject(Permission.MEASUREMENT_READ, user.projectId, user.userId)
        return userMapper.fromEntity(user)
    }

    @POST
    @Path("{type}/deregister")
    fun reportDeregistration(@PathParam("type") sourceType: String, body: DeregistrationsDTO) {
        body.deregistrations.forEach { deregistration ->
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
