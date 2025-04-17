package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.ktor.auth.ClientCredentialsConfig
import org.radarbase.ktor.auth.clientCredentials
import org.radarbase.management.client.MPClient
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.function.Supplier

class MPClientFactory(
    @Context private val config: AuthorizerConfig,
) : Supplier<MPClient> {

    override fun get(): MPClient {
        val baseUrl = config.auth.managementPortalUrl.trimEnd('/')
        val clientId = config.auth.clientId
        val clientSecret = config.auth.clientSecret ?: throw IllegalArgumentException("Client Secret is required")
        val customTokenUrl = config.auth.authUrl ?: baseUrl + "/oauth/token"
        val scopes = config.auth.scopes

        val mpClientConfig = MPClient.Config().apply {
            url = baseUrl

            auth {
                val authConfig = ClientCredentialsConfig(
                    tokenUrl = customTokenUrl,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    scope = scopes,
                    audience = "res_ManagementPortal",
                ).copyWithEnv()

                return@auth clientCredentials(
                    authConfig = authConfig,
                    targetHost = URI.create(baseUrl).host,
                )
            }
        }

        return MPClient(mpClientConfig)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MPClientFactory::class.java)
    }
}
