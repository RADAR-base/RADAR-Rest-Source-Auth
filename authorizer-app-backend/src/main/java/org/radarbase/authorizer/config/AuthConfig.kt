package org.radarbase.authorizer.config

data class AuthConfig(
    var managementPortalUrl: String = "http://managementportal-app:8080/managementportal",
    var clientId: String = "radar_rest_sources_auth_backend",
    var clientSecret: String? = null,
    var jwtECPublicKeys: List<String>? = null,
    var jwtRSAPublicKeys: List<String>? = null,
    var jwtIssuer: String? = null,
    var jwtResourceName: String = "res_restAuthorizer",
)
