package org.radarbase.authorizer.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.url
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.http.headers
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import jakarta.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.management.client.MPOrganization
import org.radarbase.management.client.MPProject
import org.radarbase.management.client.MPSubject
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

@Singleton
class MPClient(
    private val config: AuthorizerConfig,
) {
    private val logger = LoggerFactory.getLogger(MPClient::class.java)
    private val httpClient =
        HttpClient(CIO) {
            install(ContentNegotiation) {
                json(Json { ignoreUnknownKeys = true })
            }
            install(HttpTimeout) {
                val millis = 30.seconds.inWholeMilliseconds
                connectTimeoutMillis = millis
                socketTimeoutMillis = millis
                requestTimeoutMillis = millis
            }
        }

    private var accessToken: String = ""
    private var tokenExpiration: Long = 0

    private suspend fun fetchAccessToken(): String =
        withContext(Dispatchers.IO) {
            val response: HttpResponse =
                httpClient.submitForm(
                    url = config.auth.authUrl,
                    formParameters =
                        Parameters.build {
                            append("grant_type", "client_credentials")
                            append("client_id", config.auth.clientId)
                            append("client_secret", config.auth.clientSecret!!)
                            append("scope", "SUBJECT.READ PROJECT.READ")
                            append("audience", "res_ManagementPortal")
                        },
                )

            if (!response.status.isSuccess()) {
                logger.error("Failed to acquire access token: ${response.status}")
                throw RuntimeException("Unable to retrieve access token")
            }

            val tokenResponse = response.body<TokenResponse>()
            accessToken = tokenResponse.access_token
            tokenExpiration = System.currentTimeMillis() + (tokenResponse.expires_in * 1000) // Convert seconds to milliseconds

            accessToken
        }

    private suspend fun getAccessToken(): String {
        if (accessToken.isEmpty() || System.currentTimeMillis() >= tokenExpiration) {
            this.accessToken = fetchAccessToken()
        }
        return this.accessToken
    }

    suspend fun requestOrganizations(
        page: Int = 0,
        size: Int = Int.MAX_VALUE,
    ): List<MPOrganization> {
        val accessToken = getAccessToken()
        val response: HttpResponse =
            httpClient.get("${config.auth.managementPortalUrl}/api/organizations") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }

        if (!response.status.isSuccess()) {
            logger.error("Failed to fetch organizations: ${response.status}")
            throw RuntimeException("Failed to fetch organizations")
        }

        return response.body()
    }

    suspend fun requestProjects(
        page: Int = 0,
        size: Int = Int.MAX_VALUE,
    ): List<MPProject> {
        val accessToken = getAccessToken()
        val response: HttpResponse =
            httpClient.get("${config.auth.managementPortalUrl}/api/projects") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }

        if (!response.status.isSuccess()) {
            logger.error("Failed to fetch projects: ${response.status}")
            throw RuntimeException("Failed to fetch projects")
        }

        return response.body()
    }

    suspend fun requestSubjects(
        projectId: String,
        page: Int = 0,
        size: Int = Int.MAX_VALUE,
    ): List<MPSubject> {
        val accessToken = getAccessToken()
        val response: HttpResponse =
            httpClient.get("${config.auth.managementPortalUrl}/api/projects/$projectId/subjects") {
                headers {
                    append(HttpHeaders.Authorization, "Bearer $accessToken")
                }
            }

        if (!response.status.isSuccess()) {
            logger.error("Failed to fetch subjects: ${response.status}")
            throw RuntimeException("Failed to fetch subjects")
        }

        return response.body()
    }
}

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long,
)
