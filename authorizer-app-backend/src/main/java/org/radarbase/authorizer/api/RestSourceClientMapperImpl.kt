package org.radarbase.authorizer.api

import org.radarbase.authorizer.config.RestSourceClientConfig

class RestSourceClientMapperImpl : RestSourceClientMapper {

    override fun fromRestSourceClientConfig(config: RestSourceClientConfig) = RestSourceClientDetailsDTO (
            authorizationEndpoint = config.authorizationEndpoint,
            sourceType = config.sourceType,
            grantType = config.grantType,
            clientId = config.clientId,
            scope = config.scope,
            tokenEndpoint = config.tokenEndpoint
    )
}
