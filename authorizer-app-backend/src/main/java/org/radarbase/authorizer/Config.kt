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

import org.radarbase.authorizer.inject.ManagementPortalEnhancerFactory
import org.radarbase.jersey.config.ConfigLoader.copyEnv
import org.radarbase.jersey.config.EnhancerFactory
import org.radarbase.jersey.hibernate.config.DatabaseConfig
import java.net.URI
import java.util.*

data class Config(
    val service: AuthorizerServiceConfig = AuthorizerServiceConfig(),
    val auth: AuthConfig = AuthConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val restSourceClients: List<RestSourceClient> = emptyList()
)

data class AuthorizerServiceConfig(
    var baseUri: URI = URI.create("http://0.0.0.0:8080/rest-sources/backend/"),
    var advertisedBaseUri: URI? = null,
    var resourceConfig: Class<out EnhancerFactory> = ManagementPortalEnhancerFactory::class.java,
    var enableCors: Boolean? = false,
    var syncProjectsIntervalMin: Long = 30,
    var syncParticipantsIntervalMin: Long = 30,
    val stateStoreExpiryInMin: Long = 5
)

data class AuthConfig(
    var managementPortalUrl: String = "http://managementportal-app:8080/managementportal/",
    var clientId: String = "radar_rest_sources_auth_backend",
    var clientSecret: String? = null,
    var jwtECPublicKeys: List<String>? = null,
    var jwtRSAPublicKeys: List<String>? = null,
    var jwtIssuer: String? = null,
    var jwtResourceName: String = "res_restAuthorizer"
)

data class RestSourceClient(
    val sourceType: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val clientId: String? = null,
    val clientSecret: String? = null,
    val grantType: String? = null,
    val scope: String? = null
) {
    fun withEnv(): RestSourceClient = this
        .copyEnv("${sourceType.toUpperCase(Locale.US)}_CLIENT_ID") { copy(clientId = it) }
        .copyEnv("${sourceType.toUpperCase(Locale.US)}_CLIENT_SECRET") { copy(clientSecret = it) }
}

data class RestSourceClients(
    val clients: List<RestSourceClient>
)
