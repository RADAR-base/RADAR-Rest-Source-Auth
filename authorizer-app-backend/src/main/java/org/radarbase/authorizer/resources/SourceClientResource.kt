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

import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.*
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.util.StateStore
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarcns.auth.authorization.Permission
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Path("source-clients")
@Produces(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class SourceClientResource(
    @Context private val restSourceClients: RestSourceClients,
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
    fun clients(): ShareableClientDetails = sharableClientDetails

    @GET
    @Authenticated
    @Path("type")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun types(): List<String> = sourceTypes

    @GET
    @Authenticated
    @Path("{type}")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun client(@PathParam("type") type: String): ShareableClientDetail {
        val sourceType = sharableClientDetails.sourceClients.find { it.sourceType == type }
            ?: throw HttpNotFoundException("source-type-not-found", "Client with source-type $type is not configured")

        return sourceType.copy(state = stateStore.generate(type).stateId)
    }

    @GET
    @Authenticated
    @Path("{type}/auth-endpoint")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun getAuthEndpoint(@PathParam("type") type: String, @QueryParam("callbackUrl") callbackUrl: String): String {
        return authorizationService.getAuthorizationEndpointWithParams(type, callbackUrl);
    }

    @DELETE
    @Authenticated
    @Path("{type}/authorization/{serviceUserId}")
    @NeedsPermission(Permission.Entity.SUBJECT, Permission.Operation.UPDATE)
    fun deleteAuthorizationWithToken(
        @PathParam("serviceUserId") serviceUserId: String,
        @PathParam("sourceType") sourceType: String,
        @QueryParam("accessToken") accessToken: String?,
    ): Boolean {
        val user = userRepository.findByExternalId(serviceUserId, sourceType)
        if (user == null) {
            if (!accessToken.isNullOrEmpty()) {
                logger.info("No user found for external ID provided. Continuing deregistration..")
                return authorizationService.revokeToken(serviceUserId, sourceType, accessToken)
            } else throw HttpNotFoundException("user-not-found", "User and access token not valid")
        } else {
            auth.checkPermissionOnSubject(Permission.SUBJECT_UPDATE, user.projectId, user.userId)
            return authorizationService.revokeToken(user)
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
    fun reportDeregistration(@PathParam("type") sourceType: String, body: DeregistrationsDTO): Response {
        body.deregistrations.forEach { it ->
            val user = userRepository.findByExternalId(it.userId, sourceType)
            if (user != null && user.accessToken == it.userAccessToken)
                authorizationService.deregisterUser(user)
        }

        return Response.ok().build()
    }

    companion object {
        val logger: Logger = LoggerFactory.getLogger(SourceClientResource::class.java)
    }

}
