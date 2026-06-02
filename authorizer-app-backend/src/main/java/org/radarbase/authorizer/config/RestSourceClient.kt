package org.radarbase.authorizer.config

import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GARMIN_AUTH
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GOOGLE_AUTH
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
    val oauthVersion: String = "oauth2",
) {
    val usesOauth2: Boolean
        get() = when {
            oauthVersion.equals("oauth2", ignoreCase = true) -> true
            oauthVersion.equals("oauth1", ignoreCase = true) -> false
            else -> throw IllegalArgumentException(
                "Invalid OAuth version for $sourceType: '$oauthVersion'. Expected 'oauth1' or 'oauth2'.",
            )
        }

    val usesPkce: Boolean
        get() = when {
            sourceType == GARMIN_AUTH && usesOauth2 -> true
            sourceType == GOOGLE_AUTH -> true
            else -> false
        }

    fun withEnv(): RestSourceClient =
        this.copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_ID") { copy(clientId = it) }
            .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_SECRET") { copy(clientSecret = it) }
            .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_AUTH_URL") { copy(authorizationEndpoint = it) }
            .copyEnv("${sourceType.uppercase(Locale.US)}_CLIENT_TOKEN_URL") { copy(tokenEndpoint = it) }
}
