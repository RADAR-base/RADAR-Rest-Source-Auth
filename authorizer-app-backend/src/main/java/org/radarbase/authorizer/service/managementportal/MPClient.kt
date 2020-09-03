/*
 *  Copyright 2020 The Hyve
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.radarbase.authorizer.service.managementportal

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import org.radarbase.authorizer.Config
import org.radarbase.authorizer.api.Project
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.User
import org.radarbase.authorizer.util.requestValue
import org.radarbase.jersey.auth.Auth
import org.slf4j.LoggerFactory
import java.net.MalformedURLException
import java.time.Duration
import java.time.Instant
import javax.ws.rs.core.Context

class MPClient(
        @Context config: Config,
        @Context private val auth: Auth,
        @Context private val objectMapper: ObjectMapper,
        @Context private val httpClient: OkHttpClient
) {
    private val clientId: String = config.auth.clientId
    private val clientSecret: String = config.auth.clientSecret
        ?: throw IllegalArgumentException("Cannot configure managementportal client without client secret")
    private val baseUrl: HttpUrl = config.auth.managementPortalUrl.toHttpUrlOrNull()
        ?: throw MalformedURLException("Cannot parse base URL ${config.auth.managementPortalUrl} as an URL")
    private val projectListReader = objectMapper.readerFor(object : TypeReference<List<ProjectDto>>() {})
    private val userListReader = objectMapper.readerFor(object : TypeReference<List<SubjectDto>>() {})
    private val tokenReader = objectMapper.readerFor(RestOauth2AccessToken::class.java)

    private var token: String? = null
    private var expiration: Instant? = null

    private val validToken: String?
        get() {
            val localToken = token ?: return null
            expiration?.takeIf { it > Instant.now() } ?: return null
            return localToken
        }

    private fun ensureToken(): String {
        val localToken = validToken

        return if (localToken != null) {
            localToken
        } else {
            val request = Request.Builder().apply {
                url(baseUrl.resolve("oauth/token")!!)
                post(FormBody.Builder().apply {
                    add("grant_type", "client_credentials")
                    add("client_id", clientId)
                    add("client_secret", clientSecret)
                }.build())
                header("Authorization", Credentials.basic(clientId, clientSecret))
            }.build()

            httpClient.requestValue<RestOauth2AccessToken>(request, tokenReader).let {
                expiration = Instant.now() + Duration.ofSeconds(it.expiresIn.toLong()) - Duration.ofMinutes(5)
                token = it.accessToken
                it.accessToken
            }
        }
    }

    fun readProjects(): List<Project> {
        logger.debug("Requesting for projects")
        val request = Request.Builder().apply {
            url(baseUrl.resolve("api/projects")!!)
            header("Authorization", "Bearer ${ensureToken()}")
        }.build()

        return httpClient.requestValue<List<ProjectDto>>(request, projectListReader)
            .map {
                Project(
                    id = it.id,
                    name = it.name,
                    location = it.location,
                    organization = it.organization,
                    description = it.description)
            }
    }

    fun readParticipants(projectId: String): List<User> {
        val request = Request.Builder().apply {
            url(baseUrl.newBuilder()
                .addPathSegments("api/projects/$projectId/subjects")
                .addQueryParameter("page", "0")
                .addQueryParameter("size", Int.MAX_VALUE.toString())
                .build())
            header("Authorization", "Bearer ${ensureToken()}")
        }.build()

        return httpClient.requestValue<List<SubjectDto>>(request, userListReader)
            .map {
                User(
                    id = it.login,
                    projectId = projectId,
                    externalId = it.externalId,
                    status = it.status)
            }
    }

    data class SubjectDto(
            val login: String,
            val externalId: String? = null,
            val status: String = "DEACTIVATED",
            val attributes: Map<String, String> = mapOf())

    data class ProjectDto(
            @JsonProperty("projectName") val id: String,
            @JsonProperty("humanReadableProjectName") val name: String? = null,
            val location: String? = null,
            val organization: String? = null,
            val description: String? = null)

    companion object {
        private val logger = LoggerFactory.getLogger(MPClient::class.java)
    }
}
