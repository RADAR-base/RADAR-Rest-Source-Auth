package org.radarbase.authorizer.config

import io.ktor.http.*
import org.radarbase.authorizer.enhancer.ManagementPortalEnhancerFactory
import org.radarbase.jersey.enhancer.EnhancerFactory
import java.net.URI
import kotlin.time.Duration.Companion.days

data class AuthorizerServiceConfig(
    val baseUri: URI = URI.create("http://0.0.0.0:8080/rest-sources/backend/"),
    val advertisedBaseUri: URI? = null,
    val frontendBaseUri: URI? = null,
    val resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
    val enableCors: Boolean? = false,
    val syncProjectsIntervalMin: Long = 30,
    val syncParticipantsIntervalMin: Long = 30,
    val tokenExpiryTimeInMinutes: Long = 15,
    val persistentTokenExpiryInMin: Long = 3.days.inWholeMinutes,
) {
    val callbackUrl: Url by lazy {
        val frontendBaseUrlBuilder = when {
            frontendBaseUri != null -> URLBuilder().takeFrom(frontendBaseUri)
            advertisedBaseUri != null -> {
                URLBuilder().apply {
                    takeFrom(advertisedBaseUri)
                    pathSegments = buildList(pathSegments.size) {
                        addAll(pathSegments.dropLastWhile { it.isEmpty() || it == "backend" })
                        add("authorizer")
                    }
                }
            }
            else -> throw IllegalStateException("Frontend URL parameter is not a valid HTTP URL.")
        }
        frontendBaseUrlBuilder
            .appendPathSegments("users:new")
            .build()
    }
}
