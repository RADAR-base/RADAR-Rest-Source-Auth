package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.RestSourceClients
import org.radarbase.jersey.exception.HttpBadRequestException
import org.radarbase.jersey.exception.HttpNotFoundException

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

    operator fun contains(sourceType: String): Boolean = sourceType in configMap

    fun ensureSourceType(sourceType: String) {
        if (sourceType !in this) {
            throw HttpNotFoundException(
                "source-type-not-found",
                "Source type $sourceType was not configured.",
            )
        }
    }
}
