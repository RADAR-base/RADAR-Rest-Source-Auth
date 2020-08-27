package org.radarbase.authorizer.api

import org.radarbase.authorizer.config.RestSourceClientConfig

interface RestSourceClientMapper {
    fun fromRestSourceClientConfig(config: RestSourceClientConfig): RestSourceClientDetailsDTO
}
