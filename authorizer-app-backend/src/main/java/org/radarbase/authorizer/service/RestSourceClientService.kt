package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.jersey.exception.HttpBadRequestException

class RestSourceClientService(
    @Context restSourceClients: RestSourceClients,
) {
    val clients = restSourceClients.clients
    private val configMap = clients.associateBy { it.sourceType }

    fun forSourceType(sourceType: String) = configMap[sourceType]
        ?: throw HttpBadRequestException(
            "client-config-not-found",
            "Cannot find client configurations for source-type $sourceType",
        )
}
