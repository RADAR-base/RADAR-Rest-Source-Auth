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

package org.radarbase.authorizer

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.radarbase.authorizer.inject.ManagementPortalEnhancerFactory
import org.radarbase.jersey.config.ConfigLoader.copyEnv
import org.radarbase.jersey.config.EnhancerFactory
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import java.net.URI
import java.time.Duration
import java.util.*

data class Config(
    val service: AuthorizerServiceConfig = AuthorizerServiceConfig(),
    val auth: AuthConfig = AuthConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val restSourceClients: List<RestSourceClient> = emptyList(),
)

data class AuthorizerServiceConfig(
    var baseUri: URI = URI.create("http://0.0.0.0:8080/rest-sources/backend/"),
    val advertisedBaseUri: URI? = null,
    val frontendBaseUri: URI? = null,
    var resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
    var enableCors: Boolean? = false,
    var syncProjectsIntervalMin: Long = 30,
    var syncParticipantsIntervalMin: Long = 30,
    val tokenExpiryTimeInMinutes: Long = 15,
    val persistentTokenExpiryInMin: Long = Duration.ofDays(3).toMinutes(),
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

data class AuthConfig(
    var managementPortalUrl: String = "http://managementportal-app:8080/managementportal/",
    var clientId: String = "radar_rest_sources_auth_backend",
    var clientSecret: String? = null,
    var jwtECPublicKeys: List<String>? = null,
    var jwtRSAPublicKeys: List<String>? = null,
    var jwtIssuer: String? = null,
    var jwtResourceName: String = "res_restAuthorizer",
)

data class RestSourceClient(
    val sourceType: String,
    val preAuthorizationEndpoint: String?,
    val authorizationEndpoint: String,
    val deregistrationEndpoint: String?,
    val tokenEndpoint: String,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val grantType: String? = null,
    val scope: String? = null,
    val state: String? = null,
) {
    fun withEnv(): RestSourceClient = this
        .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_ID") { copy(clientId = it) }
        .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_SECRET") { copy(clientSecret = it) }
}

data class RestSourceClients(
    val clients: List<RestSourceClient>,
)
