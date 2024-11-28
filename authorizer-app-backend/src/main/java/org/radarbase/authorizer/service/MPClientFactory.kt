package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.ktor.auth.ClientCredentialsConfig
import org.radarbase.ktor.auth.clientCredentials
import org.radarbase.management.client.MPClient
import org.radarbase.management.client.mpClient
import org.slf4j.LoggerFactory
import java.net.URI
import java.util.function.Supplier

class MPClientFactory(
    @Context private val config: AuthorizerConfig,
) : Supplier<MPClient> {

    override fun get(): MPClient {
        val baseUrl = config.auth.managementPortalUrl
        val clientId = config.auth.clientId ?: throw IllegalArgumentException("Client ID is required")
        val clientSecret = config.auth.clientSecret!! ?: throw IllegalArgumentException("Client Secret is required")
        val customTokenUrl = config.auth.authUrl

        val mpClientConfig = MPClient.Config().apply {

            auth {
                val authConfig = ClientCredentialsConfig(
                    tokenUrl = customTokenUrl,
                    clientId = clientId,
                    clientSecret = clientSecret,
                    audience = "res_ManagementPortal",
                ).copyWithEnv()

                return@auth clientCredentials(
                    authConfig = authConfig,
                    targetHost = URI.create(baseUrl).host
                )
            }
        }

        return MPClient(mpClientConfig)
    }

    companion object {
        private val logger = LoggerFactory.getLogger(MPClientFactory::class.java)
    }
}
