package org.radarbase.authorizer.config

import org.radarbase.jersey.config.ConfigLoader.copyEnv
import java.util.Locale

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
        .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_AUTH_URL") { copy(authorizationEndpoint = it) }
        .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_TOKEN_URL") { copy(tokenEndpoint = it) }
}
