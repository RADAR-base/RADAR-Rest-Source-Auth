package org.radarbase.authorizer.config

data class RestSourceClients(
    val clients: List<RestSourceClient> = emptyList(),
)
