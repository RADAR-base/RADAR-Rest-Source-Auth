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
import org.radarbase.authorizer.service.RestSourceAuthorizationService
import org.radarbase.authorizer.service.RestSourceUserSubscriptionService
import org.radarbase.jersey.exception.HttpApplicationException
import org.radarbase.jersey.service.AsyncCoroutineService
import org.slf4j.LoggerFactory
import java.util.concurrent.Future
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.minutes
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
    @Context private val authorizationService: RestSourceAuthorizationService,
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
        // Without an explicit timeout the radar-jersey default of 30 s applies and cuts large passes short.
        // A pass may take up to one reconcile interval, so it never overlaps the next one.
        val passTimeout = ghConfig.reconcileIntervalMinutes.coerceAtLeast(1).minutes
        asyncService.runBlocking(passTimeout) {
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
        var failed = 0
        // Each user is handled on its own, so one user's failure (a busy lock, a user deleted during the pass,
        // a database error) never stops the pass for the others.
        for (user in authorized) {
            try {
                subscriptionService.withUserLock(user) {
                    // Read the user again under the lock: it may have been deleted, revoked or re-authorized
                    // since the list above.
                    val current = userRepository.read(user.id!!)
                    val externalId = current?.externalUserId
                    if (current == null || !current.authorized || externalId == null) return@withUserLock
                    val remoteSub = remoteByUser[externalId]
                    if (remoteSub == null) {
                        // Google does not list a subscription, so the local flag must not claim one.
                        if (current.subscription?.isSubscribed == true) {
                            subscriptionRepository.markSubscribed(current.id!!, externalId, false)
                        }
                        val result = subscriptionService.subscribeResult(current)
                        if (result.isSuccess) {
                            created++
                        } else if (result is SubscriptionResult.PermanentFailure && result.code == 403) {
                            checkAuthorization(current)
                        }
                    } else {
                        if (remoteSub.dataTypeIds.toSet() != desiredDataTypes) {
                            if (client.patchSubscription(remoteSub.name, remoteSub.user, ghConfig.dataTypes).isSuccess) {
                                patched++
                            }
                        }
                        // Make sure the local flag reflects reality.
                        if (current.subscription?.isSubscribed != true) {
                            subscriptionRepository.markSubscribed(current.id!!, externalId, true)
                        }
                    }
                }
            } catch (ex: Exception) {
                failed++
                logger.warn("Reconcile failed for user={}", user.userId, ex)
            }
        }

        val orphans = remote.filter { it.healthUserId != null && it.healthUserId !in authorizedIds }
        val (deleted, deleteFailed) = deleteOrphans(orphans, localByExternalId)

        logger.info(
            "Google Health subscription reconcile complete: {} authorized user(s), created={}, patched={}, " +
                "orphansDeleted={}, failed={}.",
            authorized.size,
            created,
            patched,
            deleted,
            failed + deleteFailed,
        )
    }

    /**
     * Google refused to create the subscription (HTTP 403). That happens when the user withdrew consent. Such
     * a user stays authorized here until a token refresh fails, and nothing asks for a token once no data
     * arrives, so the create would be refused every pass. One refresh settles it: a revoked grant goes through
     * the refused-refresh path, which unsubscribes the user and marks them unauthorized. Runs with the user's
     * token lock held.
     */
    private suspend fun checkAuthorization(user: RestSourceUser) {
        try {
            val token = authorizationService.refreshToken(user)
            userRepository.updateToken(token, user)
            if (token != null) {
                logger.warn(
                    "Google refused the subscription of user={} although their token is still valid. " +
                        "Check the service account and the subscriber configuration.",
                    user.userId,
                )
            } else {
                logger.info("User={} has no usable refresh token, marked unauthorized.", user.userId)
            }
        } catch (ex: HttpApplicationException) {
            logger.info("User={} is no longer authorized: {}", user.userId, ex.message)
        } catch (ex: Exception) {
            logger.warn("Could not check the authorization of user={} after a refused subscription", user.userId, ex)
        }
    }

    /**
     * Deletes subscriptions whose user is no longer authorized here, and clears the matching local
     * flag so that a reported subscription state never outlives the subscription itself. A withdrawn
     * user is removed from this service entirely, so they fall out of the authorized set and their
     * leftover subscription is cleaned up here. Returns the number of deleted and failed subscriptions.
     */
    private suspend fun deleteOrphans(
        orphans: List<RemoteSubscription>,
        localByExternalId: Map<String, RestSourceUser>,
    ): Pair<Int, Int> {
        val maxDeletes = ghConfig.reconcileMaxDeletesPerPass
        if (maxDeletes <= 0) {
            if (orphans.isNotEmpty()) {
                logger.info(
                    "Orphan deletion is disabled (reconcileMaxDeletesPerPass={}), keeping {} orphaned subscription(s).",
                    maxDeletes,
                    orphans.size,
                )
            }
            return 0 to 0
        }
        if (orphans.size > maxDeletes) {
            logger.warn(
                "Reconcile would delete {} orphaned subscription(s), over the safety cap ({}). " +
                    "Skipping deletion this pass — investigate before mass-removal.",
                orphans.size,
                maxDeletes,
            )
            return 0 to 0
        }

        var deleted = 0
        var failed = 0
        for (orphan in orphans) {
            try {
                val localUser = localByExternalId[orphan.healthUserId]
                val done = if (localUser == null) {
                    deleteOrphan(orphan, null)
                } else {
                    // Under the user's lock, and only if the user is still not authorized: they may have
                    // re-authorized since the list at the start of this pass.
                    subscriptionService.withUserLock(localUser) {
                        val current = userRepository.read(localUser.id!!)
                        if (current != null && current.authorized) {
                            logger.info("Keeping subscription of user={}, re-authorized during the pass", current.userId)
                            false
                        } else {
                            deleteOrphan(orphan, current)
                        }
                    } ?: false
                }
                if (done) deleted++
            } catch (ex: Exception) {
                failed++
                logger.warn("Reconcile could not delete the orphaned subscription of user={}", orphan.healthUserId, ex)
            }
        }
        return deleted to failed
    }

    private suspend fun deleteOrphan(orphan: RemoteSubscription, user: RestSourceUser?): Boolean =
        when (val result = client.deleteByName(orphan.name)) {
            is SubscriptionResult.Success -> {
                logger.info("Reconcile deleted orphaned subscription user={}", orphan.healthUserId)
                clearLocalFlag(user)
                true
            }

            else -> {
                logger.warn("Reconcile delete failed user={}: {}", orphan.healthUserId, result)
                false
            }
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
