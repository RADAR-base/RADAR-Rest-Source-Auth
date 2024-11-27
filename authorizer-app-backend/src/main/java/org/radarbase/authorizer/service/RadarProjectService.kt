package org.radarbase.authorizer.service

import io.ktor.http.HttpStatusCode
import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.service.ProjectService
import org.radarbase.kotlin.coroutines.CacheConfig
import org.radarbase.kotlin.coroutines.CachedMap
import org.radarbase.management.client.HttpStatusException
import org.radarbase.management.client.MPOrganization
import org.radarbase.management.client.MPProject
import org.radarbase.management.client.MPSubject
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import kotlin.time.Duration.Companion.minutes
import kotlin.time.toKotlinDuration

class RadarProjectService(
    @Context private val config: AuthorizerConfig,
    @Context private val mpClient: MPClient,
) : ProjectService {

    private val projects: CachedMap<String, MPProject>
    private val organizations: CachedMap<String, MPOrganization>
    private val participants: ConcurrentMap<String, CachedMap<String, MPSubject>> =
        ConcurrentHashMap()

    private val projectCacheConfig =
        CacheConfig(
            refreshDuration = Duration.ofMinutes(5).toKotlinDuration(),
            retryDuration = RETRY_INTERVAL,
        )

    init {
        val cacheConfig =
            CacheConfig(
                refreshDuration = Duration.ofMinutes(5).toKotlinDuration(),
                retryDuration = 1.minutes,
            )

        projects =
            CachedMap(cacheConfig) {
                try {
                    val projectList = mpClient.requestProjects()
                    projectList.associateBy { it.id }
                } catch (e: Exception) {
                    logger.error("Failed to fetch projects from Management Portal", e)
                    throw RuntimeException("Unable to fetch projects", e)
                }
            }

        organizations =
            CachedMap(cacheConfig) {
                try {
                    mpClient.requestOrganizations().associateBy { it.id }.also {
                        logger.debug("Fetched organizations {}", it)
                    }
                } catch (ex: HttpStatusException) {
                    if (ex.code == HttpStatusCode.NotFound) {
                        logger.warn(
                            "Target ManagementPortal does not support organizations. Using default organization main.",
                        )
                        mapOf("main" to defaultOrganization)
                    } else {
                        throw ex
                    }
                }
            }
    }

    suspend fun userProjects(): List<MPProject> = projects.get().values.toList()

    override suspend fun ensureSubject(
        projectId: String,
        userId: String,
    ) {
        ensureProject(projectId)
        if (!projectUserCache(projectId).contains(userId)) {
            throw HttpNotFoundException(
                "user_not_found",
                "User $userId not found in project $projectId of ManagementPortal.",
            )
        }
    }

    override suspend fun ensureOrganization(organizationId: String) {
        if (!organizations.contains(organizationId)) {
            throw HttpNotFoundException(
                "organization_not_found",
                "Organization $organizationId not found in Management Portal.",
            )
        }
    }

    override suspend fun ensureProject(projectId: String) {
        if (!projects.contains(projectId)) {
            throw HttpNotFoundException(
                "project_not_found",
                "Project $projectId not found in Management Portal.",
            )
        }
    }

    override suspend fun projectOrganization(projectId: String): String =
        projects.get(projectId)?.organization?.id
            ?: throw HttpNotFoundException(
                "project_not_found",
                "Project $projectId not found in Management Portal.",
            )

    suspend fun project(projectId: String): MPProject =
        projects.get(projectId)
            ?: throw HttpNotFoundException(
                "project_not_found",
                "Project $projectId not found in Management Portal.",
            )

    override suspend fun listProjects(organizationId: String): List<String> =
        projects
            .get()
            .asSequence()
            .filter { it.value.organization?.id == organizationId }
            .mapTo(ArrayList()) { it.key }

    suspend fun projectSubjects(projectId: String): List<MPSubject> = projectUserCache(projectId).get().values.toList()

    private suspend fun projectUserCache(projectId: String) =
        participants.computeIfAbsent(projectId) {
            CachedMap(projectCacheConfig) {
                mpClient.requestSubjects(projectId).associateBy { checkNotNull(it.id) }
            }
        }

    suspend fun subject(
        projectId: String,
        userId: String,
    ): MPSubject? {
        ensureProject(projectId)
        return projectUserCache(projectId).get(userId)
    }

    companion object {
        private val RETRY_INTERVAL = 1.minutes
        private val logger = LoggerFactory.getLogger(ProjectService::class.java)
        private val defaultOrganization = MPOrganization("main")
    }
}
