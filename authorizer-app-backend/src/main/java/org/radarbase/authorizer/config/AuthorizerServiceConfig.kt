package org.radarbase.authorizer.config

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.radarbase.authorizer.inject.ManagementPortalEnhancerFactory
import org.radarbase.jersey.config.EnhancerFactory
import java.net.URI

data class AuthorizerServiceConfig(
    var baseUri: URI = URI.create("http://0.0.0.0:8080/rest-sources/backend/"),
    val advertisedBaseUri: URI? = null,
    val frontendBaseUri: URI? = null,
    var resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
    var enableCors: Boolean? = false,
    var syncProjectsIntervalMin: Long = 30,
    var syncParticipantsIntervalMin: Long = 30,
    val stateStoreExpiryInMin: Long = 5,
) {
    val callbackUrl: HttpUrl by lazy {
        val frontendBaseUrlBuilder = when {
            frontendBaseUri != null -> frontendBaseUri.toHttpUrlOrNull()?.newBuilder()
            advertisedBaseUri != null -> {
                advertisedBaseUri.toHttpUrlOrNull()?.let { advertisedUrl ->
                    advertisedUrl.newBuilder().apply {
                        advertisedUrl.pathSegments.asReversed()
                            .forEachIndexed { idx, segment ->
                                if (segment.isEmpty() || segment == "backend") {
                                    removePathSegment(advertisedUrl.pathSize - 1 - idx)
                                }
                            }
                        addPathSegment("authorizer")
                    }
                }
            }
            else -> null
        }
        checkNotNull(frontendBaseUrlBuilder) { "Frontend URL parameter $frontendBaseUri is not a valid HTTP URL." }
            .addPathSegment("users:new")
            .build()
    }
}
