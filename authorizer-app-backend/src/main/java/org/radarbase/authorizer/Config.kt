package org.radarbase.authorizer

import org.radarbase.authorizer.inject.ManagementPortalEnhancerFactory
import org.radarbase.jersey.config.EnhancerFactory
import java.net.URI

data class Config(
    val service: AuthorizerServiceConfig = AuthorizerServiceConfig(),
    val auth: AuthConfig = AuthConfig(),
    val database: DatabaseConfig = DatabaseConfig()
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
    var jdbcDriver: String? = "org.h2.Driver",
    var jdbcUrl: String? = null,
    var jdbcUser: String? = null,
    var jdbcPassword: String? = null,
    var hibernateDialect: String = "org.hibernate.dialect.PostgreSQLDialect",
    var additionalPersistenceConfig: Map<String, String>? = null
)
