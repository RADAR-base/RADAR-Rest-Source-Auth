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

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RestSourceClientMapper
import org.radarbase.authorizer.api.ShareableClientDetail
import org.radarbase.authorizer.api.ShareableClientDetails
import org.radarbase.authorizer.service.RestSourceAuthorizationServiceFactory
import org.radarbase.authorizer.util.StateStore
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarcns.auth.authorization.Permission
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("source-clients")
@Produces(MediaType.APPLICATION_JSON)
@Resource
@Authenticated
@Singleton
class SourceClientResource(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val clientMapper: RestSourceClientMapper,
    @Context private val stateStore: StateStore,
    @Context private val auth: Auth
) {

    private val sourceTypes = restSourceClients.clients.map { it.sourceType }

    private val sharableClientDetails = clientMapper.fromSourceClientConfigs(restSourceClients.clients)

    private val authorizationServiceFactory: RestSourceAuthorizationServiceFactory = RestSourceAuthorizationServiceFactory(restSourceClients, httpClient = OkHttpClient(), objectMapper = ObjectMapper(), stateStore = stateStore)

    @GET
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun clients(): ShareableClientDetails = sharableClientDetails

    @GET
    @Path("type")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun types(): List<String> = sourceTypes

    @GET
    @Path("{type}")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun client(@PathParam("type") type: String): ShareableClientDetail {
        val sourceType = sharableClientDetails.sourceClients.find { it.sourceType == type }
                ?: throw HttpNotFoundException("source-type-not-found", "Client with source-type $type is not configured")

        return sourceType.copy(state = stateStore.generate(type).stateId)
    }

    @GET
    @Path("{type}/auth-endpoint")
    @NeedsPermission(Permission.Entity.SOURCETYPE, Permission.Operation.READ)
    fun getAuthEndpoint(@PathParam("type") type: String, @QueryParam("callbackUrl") callbackUrl: String): String {
        RestSourceUserResource.logger.info("Getting auth endpoint")
        val result = authorizationServiceFactory.getAuthorizationService(type).getAuthorizationEndpointWithParams(type, callbackUrl)

        return result;
    }



}
