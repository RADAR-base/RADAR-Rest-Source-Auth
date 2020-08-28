package org.radarbase.authorizer

import org.radarbase.authorizer.inject.ManagementPortalEnhancerFactory
import org.radarbase.jersey.config.EnhancerFactory
import java.net.URI

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
    var syncParticipantsIntervalMin: Long = 30
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

data class DatabaseConfig(
    val jdbcDriver: String? = "org.h2.Driver",
    val jdbcUrl: String? = null,
    val jdbcUser: String? = null,
    val jdbcPassword: String? = null,
    val hibernateDialect: String = "org.hibernate.dialect.PostgreSQLDialect",
    val additionalPersistenceConfig: Map<String, String>? = null
)

data class RestSourceClient(
    val sourceType: String,
    val authorizationEndpoint: String,
    val tokenEndpoint: String,
    val clientId: String,
    val clientSecret: String,
    val grantType: String? = null,
    val scope: String? = null
)

data class RestSourceClients(
    val clients: List<RestSourceClient>
)
