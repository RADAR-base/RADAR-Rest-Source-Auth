package org.radarbase.authorizer.config

data class AuthConfig(
    val managementPortalUrl: String = "http://management-portal:8080/managementportal",
    val authUrl: String? = null,
    val clientId: String = "radar_rest_sources_auth_backend",
    val clientSecret: String? = "",
    val jwtECPublicKeys: List<String>? = null,
    val jwtRSAPublicKeys: List<String>? = null,
    val jwtIssuer: String? = null,
    val jwtResourceName: String = "res_restAuthorizer",
    val jwksUrls: List<String> = emptyList(),
)
