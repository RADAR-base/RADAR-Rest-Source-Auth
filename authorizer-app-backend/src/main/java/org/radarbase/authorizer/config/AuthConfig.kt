package org.radarbase.authorizer.config

data class AuthConfig(
    val managementPortalUrl: String = "http://managementportal-app:8080/managementportal",
    val authUrl: String = "http://hydra-public:4444/oauth2/token",
    val clientId: String = "radar_rest_sources_auth_backend",
    val clientSecret: String? = "",
    val jwtECPublicKeys: List<String>? = null,
    val jwtRSAPublicKeys: List<String>? = null,
    val jwtIssuer: String? = null,
    val jwtResourceName: String = "res_restAuthorizer",
    val jwksUrls: List<String> = listOf("http://hydra-admin:4445/admin/keys/hydra.jwt.access-token"),
)
