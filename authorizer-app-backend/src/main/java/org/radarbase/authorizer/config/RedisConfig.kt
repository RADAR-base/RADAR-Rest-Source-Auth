package org.radarbase.authorizer.config

import java.net.URI

data class RedisConfig(
    val uri: URI = URI("redis://localhost:6379"),
    val lockPrefix: String = "radar-rest-sources-backend/lock",
)
