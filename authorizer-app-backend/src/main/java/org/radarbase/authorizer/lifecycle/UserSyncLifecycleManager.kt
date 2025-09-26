package org.radarbase.authorizer.lifecycle

import jakarta.inject.Singleton
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.ext.Provider
import org.glassfish.jersey.server.BackgroundScheduler
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.jersey.service.managementportal.RadarProjectService
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Provider
@Singleton
class UserSyncLifecycleManager(
    @BackgroundScheduler
    @Context
    private val scheduler: ScheduledExecutorService,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val projectService: RadarProjectService,
    @Context private val authorizationService: RestSourceAuthorizationService,
    @Context private val asyncService: AsyncCoroutineService,
    @Context private val config: AuthorizerConfig,
) : ApplicationEventListener {

    private var cleanupTask: Future<*>? = null

    override fun onEvent(event: ApplicationEvent?) {
        event ?: return
        when (event.type) {
            ApplicationEvent.Type.INITIALIZATION_APP_FINISHED -> schedulePeriodicCleanup()
            ApplicationEvent.Type.DESTROY_FINISHED -> cancelPeriodicCleanup()
            else -> Unit
        }
    }

    @Synchronized
    private fun schedulePeriodicCleanup() {
        if (cleanupTask != null) return

        val intervalMin = config.service.syncParticipantsIntervalMin
        cleanupTask = scheduler.scheduleAtFixedRate(
            ::runCleanup,
            intervalMin, // initial delay
            intervalMin,
            TimeUnit.MINUTES,
        )
        logger.info("Scheduled Management Portal user synchronization every {} minutes.", intervalMin)
    }

    @Synchronized
    private fun cancelPeriodicCleanup() {
        cleanupTask?.let {
            it.cancel(true)
            cleanupTask = null
        }
    }

    private fun runCleanup() {
        asyncService.runBlocking {
            try {
                val allUsers = userRepository.listAll()
                if (allUsers.isEmpty()) return@runBlocking

                val usersByProject = allUsers
                    .filter { it.projectId != null && it.userId != null }
                    .groupBy { it.projectId!! }

                for ((projectId, users) in usersByProject) {
                    val mpSubjects = try {
                        projectService.projectSubjects(projectId)
                    } catch (ex: Exception) {
                        logger.warn("Skipping cleanup for project {} due to MP error: {}", projectId, ex.message)
                        continue
                    }
                    val mpUserIds = mpSubjects.mapNotNull { it.id }.toHashSet()

                    for (user in users) {
                        val subjectId = user.userId
                        if (subjectId != null && subjectId !in mpUserIds) {
                            removeUser(user)
                        }
                    }
                }
            } catch (ex: Throwable) {
                logger.error("Failed to run Management Portal user cleanup.", ex)
            }
        }
    }

    private suspend fun removeUser(user: RestSourceUser) {
        try {
            logger.info("Removing user {} from project {} (sourceType={}) as it no longer exists in Management Portal.", user.userId, user.projectId, user.sourceType)
            // TODO: Delegate to the source-specific authorization service to ensure proper deregistration.
            userRepository.delete(user)
        } catch (ex: Exception) {
            logger.error("Failed to remove user {} in project {}: {}", user.userId, user.projectId, ex.message)
        }
    }

    override fun onRequest(requestEvent: RequestEvent?): RequestEventListener? = null

    companion object {
        private val logger = LoggerFactory.getLogger(UserSyncLifecycleManager::class.java)
    }
}
