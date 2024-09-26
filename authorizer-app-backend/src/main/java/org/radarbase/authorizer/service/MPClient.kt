package org.radarbase.authorizer.service

import jakarta.inject.Singleton
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory
import kotlinx.serialization.json.Json
import org.radarbase.management.client.MPProject
import org.radarbase.management.client.MPOrganization
import org.radarbase.management.client.MPSubject
import io.ktor.client.request.forms.*
import io.ktor.client.call.*
import org.radarbase.authorizer.config.AuthorizerConfig

@Singleton
class MPClient {
    private val config: AuthorizerConfig = AuthorizerConfig()
    private val logger = LoggerFactory.getLogger(MPClient::class.java)

    private val httpClient = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(HttpTimeout) {
            requestTimeoutMillis = 30_000
        }
    }

    suspend fun getAccessToken(): String {
        val response: HttpResponse = httpClient.submitForm(
            url = config.auth.authUrl,
            formParameters = Parameters.build {
                append("grant_type", "client_credentials")
                append("client_id", "radar_rest_sources_auth_backend")
                append("client_secret", "secret")
                append("scope", "SUBJECT.READ PROJECT.READ")
                append("audience", "res_ManagementPortal")
            }
        )

        if (!response.status.isSuccess()) {
            logger.error("Failed to acquire access token: ${response.status}")
            throw RuntimeException("Unable to retrieve access token")
        }

        val tokenResponse = response.body<TokenResponse>()
        return tokenResponse.access_token
    }

    suspend fun requestOrganizations(page: Int = 0, size: Int = Int.MAX_VALUE): List<MPOrganization> {
        val accessToken = getAccessToken()
        val response: HttpResponse = httpClient.get("${config.auth.managementPortalUrl}/api/organizations") {
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

    suspend fun requestProjects(page: Int = 0, size: Int = Int.MAX_VALUE): List<MPProject> {
        val accessToken = getAccessToken()
        val response: HttpResponse = httpClient.get("${config.auth.managementPortalUrl}/api/projects") {
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

    suspend fun requestSubjects(projectId: String, page: Int = 0, size: Int = Int.MAX_VALUE,): List<MPSubject> {
        val accessToken = getAccessToken()
        val response: HttpResponse = httpClient.get("${config.auth.managementPortalUrl}/api/projects/${projectId}/subjects") {
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
}

@Serializable
data class TokenResponse(
    val access_token: String,
    val token_type: String,
    val expires_in: Long
)