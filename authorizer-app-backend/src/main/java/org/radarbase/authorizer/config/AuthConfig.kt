package org.radarbase.authorizer.config

data class AuthConfig(
    val managementPortalUrl: String = "http://host.docker.internal:8080/managementportal",
    val authUrl: String = "http://host.docker.internal:4444/oauth2/token",
    val clientId: String = "radar_rest_sources_auth_backend",
    val clientSecret: String? = "secret",
    val jwtECPublicKeys: List<String>? = null,
    val jwtRSAPublicKeys: List<String>? = null,
    val jwtIssuer: String? = null,
    val jwtResourceName: String = "res_restAuthorizer",
    val jwksUrls: List<String> = listOf("http://host.docker.internal:4445/admin/keys/hydra.jwt.access-token"),
)
