package org.radarbase.authorizer.resources

import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RestSourceClientMapper
import org.radarbase.authorizer.api.ShareableClientDetail
import org.radarbase.authorizer.api.ShareableClientDetails
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.exception.HttpNotFoundException
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.PathParam
import javax.ws.rs.Produces
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
    @Context private val auth: Auth
) {

    private val sourceTypes = restSourceClients.clients.map { it.sourceType }

    private val sharableClientDetails = clientMapper.fromSourceClientConfigs(restSourceClients.clients)

    @GET
    fun clients(): ShareableClientDetails = sharableClientDetails

    @GET
    @Path("type")
    fun types(): List<String> = sourceTypes

    @GET
    @Path("{type}")
    fun client(@PathParam("type") type: String): ShareableClientDetail {
        return sharableClientDetails.sourceClients.find { it.sourceType == type }
            ?: throw HttpNotFoundException("source-type-not-found", "Client with source-type $type is not configured")
    }
}
