package org.radarbase.authorizer.config

data class AuthConfig(
    val managementPortalUrl: String = "http://managementportal-app:8080/managementportal",
    val clientId: String = "radar_rest_sources_auth_backend",
    val clientSecret: String? = null,
    val jwtECPublicKeys: List<String>? = null,
    val jwtRSAPublicKeys: List<String>? = null,
    val jwtIssuer: String? = null,
    val jwtResourceName: String = "res_restAuthorizer",
)
