/*
 *  Copyright 2026 King's College London
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

package org.radarbase.authorizer.lifecycle

import jakarta.inject.Singleton
import jakarta.persistence.LockTimeoutException
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.ext.Provider
import org.glassfish.jersey.server.BackgroundScheduler
import org.glassfish.jersey.server.monitoring.ApplicationEvent
import org.glassfish.jersey.server.monitoring.ApplicationEventListener
import org.glassfish.jersey.server.monitoring.RequestEvent
import org.glassfish.jersey.server.monitoring.RequestEventListener
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.RestSourceUserSubscriptionRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.model.RemoteSubscription
import org.radarbase.authorizer.model.SubscriptionResult
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GOOGLE_AUTH
import org.radarbase.authorizer.service.GoogleHealthSubscriptionClient
import org.radarbase.authorizer.service.LockService
import org.radarbase.authorizer.service.RestSourceUserSubscriptionService
import org.radarbase.jersey.service.AsyncCoroutineService
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.seconds

@Provider
@Singleton
class GoogleHealthSubscriptionLifecycleManager(
    @BackgroundScheduler
    @Context
    private val scheduler: ScheduledExecutorService,
    @Context private val config: AuthorizerConfig,
    @Context private val asyncService: AsyncCoroutineService,
    @Context private val client: GoogleHealthSubscriptionClient,
    @Context private val subscriptionService: RestSourceUserSubscriptionService,
    @Context private val subscriptionRepository: RestSourceUserSubscriptionRepository,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val lockService: LockService,
) : ApplicationEventListener {

    private val ghConfig = config.googleHealth

    private var reconcileTask: Future<*>? = null

    override fun onEvent(event: ApplicationEvent?) {
        event ?: return
        when (event.type) {
            ApplicationEvent.Type.INITIALIZATION_APP_FINISHED -> startReconcile()
            ApplicationEvent.Type.DESTROY_FINISHED -> cancelReconcile()
            else -> Unit
        }
    }

    override fun onRequest(requestEvent: RequestEvent?): RequestEventListener? = null

    @Synchronized
    private fun startReconcile() {
        if (reconcileTask != null) return
        if (!ghConfig.isConfigured || !client.isConfigured) {
            logger.info("Google Health subscription management disabled or unconfigured — skipping reconcile.")
            return
        }
        val intervalMinutes = ghConfig.reconcileIntervalMinutes.coerceAtLeast(1)
        reconcileTask = scheduler.scheduleAtFixedRate(
            ::runReconcile,
            RECONCILE_INITIAL_DELAY_SECONDS, // initial delay to ensure liquibase is done.
            intervalMinutes * 60,
            TimeUnit.SECONDS,
        )
        logger.info(
            "Scheduled Google Health subscription reconcile (firstRunDelay={}s, intervalMinutes={}).",
            RECONCILE_INITIAL_DELAY_SECONDS,
            intervalMinutes,
        )
    }

    @Synchronized
    private fun cancelReconcile() {
        reconcileTask?.let {
            it.cancel(true)
            reconcileTask = null
        }
    }

    private fun runReconcile() {
        asyncService.runBlocking {
            try {
                lockService.runLocked(RECONCILE_LOCK, LOCK_TIMEOUT) { reconcile() }
            } catch (ex: LockTimeoutException) {
                logger.debug("Another replica holds the subscription reconcile lock — skipping this pass.")
            } catch (ex: Throwable) {
                logger.warn("Google Health subscription reconcile failed", ex)
            }
        }
    }

    private suspend fun reconcile() {
        val remote = client.listSubscriptions()
        val remoteByUser = remote.mapNotNull { sub -> sub.healthUserId?.let { it to sub } }.toMap()

        val googleUsers = userRepository.listAll().filter { it.sourceType == GOOGLE_AUTH && it.id != null }
        val localByExternalId = googleUsers.mapNotNull { user -> user.externalUserId?.let { it to user } }.toMap()

        val authorized = googleUsers.filter { it.authorized && it.externalUserId != null }
        val authorizedIds = authorized.mapNotNull { it.externalUserId }.toHashSet()
        val desiredDataTypes = ghConfig.dataTypes.toSet()

        var created = 0
        var patched = 0
        for (user in authorized) {
            val externalId = user.externalUserId!!
            val remoteSub = remoteByUser[externalId]
            if (remoteSub == null) {
                if (subscriptionService.subscribe(user)) created++
            } else {
                if (remoteSub.dataTypeIds.toSet() != desiredDataTypes) {
                    if (client.patchSubscription(remoteSub.name, remoteSub.user, ghConfig.dataTypes).isSuccess) {
                        patched++
                    }
                }
                // Make sure the local flag reflects reality.
                if (user.subscription?.isSubscribed != true) {
                    subscriptionRepository.markSubscribed(user.id!!, externalId, true)
                }
            }
        }

        val orphans = remote.filter { it.healthUserId != null && it.healthUserId !in authorizedIds }
        val deleted = deleteOrphans(orphans, localByExternalId)

        logger.info(
            "Google Health subscription reconcile complete: {} authorized user(s), created={}, patched={}, orphansDeleted={}.",
            authorized.size,
            created,
            patched,
            deleted,
        )
    }

    /**
     * Deletes subscriptions whose user is no longer authorized here, and clears the matching local
     * flag so that a reported subscription state never outlives the subscription itself. A withdrawn
     * user is removed from this service entirely, so they fall out of the authorized set and their
     * leftover subscription is cleaned up here.
     */
    private suspend fun deleteOrphans(
        orphans: List<RemoteSubscription>,
        localByExternalId: Map<String, RestSourceUser>,
    ): Int {
        val maxDeletes = ghConfig.reconcileMaxDeletesPerPass
        if (maxDeletes > 0 && orphans.size > maxDeletes) {
            logger.warn(
                "Reconcile would delete {} orphaned subscription(s), over the safety cap ({}). " +
                    "Skipping deletion this pass — investigate before mass-removal.",
                orphans.size,
                maxDeletes,
            )
            return 0
        }

        var deleted = 0
        for (orphan in orphans) {
            when (val result = client.deleteByName(orphan.name)) {
                is SubscriptionResult.Success -> {
                    deleted++
                    logger.info("Reconcile deleted orphaned subscription user={}", orphan.healthUserId)
                    clearLocalFlag(localByExternalId[orphan.healthUserId])
                }

                else -> logger.warn("Reconcile delete failed user={}: {}", orphan.healthUserId, result)
            }
        }
        return deleted
    }

    private suspend fun clearLocalFlag(user: RestSourceUser?) {
        val userId = user?.id ?: return
        if (user.subscription?.isSubscribed != true) return
        try {
            subscriptionRepository.markSubscribed(userId, null, false)
        } catch (ex: Exception) {
            logger.warn("Could not clear the subscription flag of user={}", user.userId, ex)
        }
    }

    companion object {
        private const val RECONCILE_LOCK = "googlehealth-subscription-reconcile"
        private const val RECONCILE_INITIAL_DELAY_SECONDS = 60L
        private val LOCK_TIMEOUT = 30.seconds
        private val logger = LoggerFactory.getLogger(GoogleHealthSubscriptionLifecycleManager::class.java)
    }
}
