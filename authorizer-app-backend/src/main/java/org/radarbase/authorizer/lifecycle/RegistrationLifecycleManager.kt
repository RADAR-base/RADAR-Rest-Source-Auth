package org.radarbase.authorizer.lifecycle

import jakarta.inject.Singleton
import jakarta.persistence.EntityManagerFactory
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.ext.Provider
import org.glassfish.jersey.server.BackgroundScheduler
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RegistrationRepository
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

@Provider
@Singleton
class RegistrationLifecycleManager(
    @BackgroundScheduler @Context private val executor: ScheduledExecutorService,
    @Context private val entityManagerFactory: EntityManagerFactory,
    @Context private val config: AuthorizerConfig,
) : ApplicationEventListener {
    private val expiryTime = Duration.ofMinutes(config.service.tokenExpiryTimeInMinutes)
        .coerceAtLeast(Duration.ofMinutes(1))
    private val lock: Any = Any()

    private var checkTask: Future<*>? = null

    override fun onEvent(event: ApplicationEvent?) {
        event ?: return
        when (event.type) {
            ApplicationEvent.Type.INITIALIZATION_APP_FINISHED -> startStaleChecks()
            ApplicationEvent.Type.DESTROY_FINISHED -> cancelStaleChecks()
            else -> Unit
        }
    }

    @Synchronized
    private fun cancelStaleChecks() {
        checkTask?.let {
            it.cancel(true)
            checkTask = null
        }
    }

    @Synchronized
    private fun startStaleChecks() {
        if (checkTask != null) {
            return
        }

        checkTask = executor.scheduleAtFixedRate(
            ::runStaleCheck,
            expiryTime.toSeconds(),
            expiryTime.multipliedBy(4L).toSeconds(),
            TimeUnit.SECONDS,
        )
    }

    private fun runStaleCheck() {
        try {
            val entityManager = entityManagerFactory.createEntityManager()
            try {
                val registrationRepository = RegistrationRepository(config) { entityManager }
                synchronized(lock) {
                    logger.debug("Cleaning up expired registrations.")
                    val numUpdated = registrationRepository.cleanUp()
                    if (numUpdated == 0) {
                        logger.debug("Did not clean up any registrations.")
                    } else {
                        logger.info("Removed {} expired registrations.", numUpdated)
                    }
                }
            } catch (ex: Exception) {
                logger.error("Failed to run reset of stale processing", ex)
            } finally {
                entityManager.close()
            }
        } catch (ex: Throwable) {
            logger.error("Failed to run reset of stale processing.", ex)
        }
    }

    override fun onRequest(requestEvent: RequestEvent?): RequestEventListener? = null

    companion object {
        private val logger = LoggerFactory.getLogger(RegistrationLifecycleManager::class.java)
    }
}
