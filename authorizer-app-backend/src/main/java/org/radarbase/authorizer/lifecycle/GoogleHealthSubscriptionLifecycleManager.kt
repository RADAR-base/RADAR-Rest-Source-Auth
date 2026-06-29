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
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GOOGLE_AUTH
import org.radarbase.authorizer.service.GoogleHealthSubscriptionClient
import org.radarbase.authorizer.service.RestSourceUserSubscriptionService
import org.radarbase.authorizer.model.SubscriptionResult
import org.radarbase.jersey.service.AsyncCoroutineService
import org.slf4j.LoggerFactory
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * One-shot reconcile of Google Health subscriptions against the authorized-user set, run shortly
 * after startup. It exists purely as a safety net for the synchronous, event-driven path:
 *
 *  - seeds/repairs subscriptions for authorized users that Google does not have one for,
 *  - re-patches subscriptions whose data types drifted from the current config (i.e. picks up a
 *    config change to [AuthorizerConfig.googleHealth] dataTypes),
 *  - deletes orphaned subscriptions whose user is no longer authorized (e.g. a delete that happened
 *    while the provider was unreachable).
 *
 * Dormant unless a service account is configured.
 */
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
) : ApplicationEventListener {

    override fun onEvent(event: ApplicationEvent?) {
        event ?: return
        if (event.type == ApplicationEvent.Type.INITIALIZATION_APP_FINISHED) {
            if (!config.googleHealth.isConfigured || !client.isConfigured) {
                logger.info("Google Health service account not configured — skipping subscription reconcile.")
                return
            }
            scheduler.schedule(::runReconcile, RECONCILE_DELAY_SECONDS, TimeUnit.SECONDS)
        }
    }

    override fun onRequest(requestEvent: RequestEvent?): RequestEventListener? = null

    private fun runReconcile() {
        asyncService.runBlocking {
            try {
                reconcile()
            } catch (ex: Throwable) {
                logger.warn("Google Health subscription startup reconcile failed", ex)
            }
        }
    }

    private suspend fun reconcile() {
        val remote = client.listSubscriptions()
        val remoteByUser = remote.mapNotNull { sub -> sub.healthUserId?.let { it to sub } }.toMap()

        val authorized = userRepository.listAll().filter {
            it.sourceType == GOOGLE_AUTH && it.authorized && it.externalUserId != null && it.id != null
        }
        val authorizedIds = authorized.mapNotNull { it.externalUserId }.toHashSet()
        val desiredDataTypes = config.googleHealth.dataTypes.toSet()

        var created = 0
        var patched = 0
        for (user in authorized) {
            val externalId = user.externalUserId!!
            val remoteSub = remoteByUser[externalId]
            if (remoteSub == null) {
                if (subscriptionService.subscribe(user)) created++
            } else {
                if (remoteSub.dataTypeIds.toSet() != desiredDataTypes) {
                    if (client.patchSubscription(remoteSub.name, config.googleHealth.dataTypes).isSuccess) patched++
                }
                // Make sure the local flag reflects reality.
                if (user.subscription?.isSubscribed != true) {
                    subscriptionRepository.markSubscribed(user.id!!, externalId, true)
                }
            }
        }

        val orphans = remote.filter { it.healthUserId != null && it.healthUserId !in authorizedIds }
        var deleted = 0
        if (orphans.size > MAX_ORPHAN_DELETES) {
            logger.warn(
                "Startup reconcile would delete {} orphaned subscription(s), over the safety cap ({}). " +
                    "Skipping deletion — investigate before mass-removal.",
                orphans.size, MAX_ORPHAN_DELETES,
            )
        } else {
            for (orphan in orphans) {
                if (client.deleteByName(orphan.name) is SubscriptionResult.Success) deleted++
            }
        }

        logger.info(
            "Google Health subscription reconcile complete: {} authorized user(s), created={}, patched={}, orphansDeleted={}.",
            authorized.size, created, patched, deleted,
        )
    }

    companion object {
        private const val RECONCILE_DELAY_SECONDS = 30L
        private const val MAX_ORPHAN_DELETES = 100
        private val logger = LoggerFactory.getLogger(GoogleHealthSubscriptionLifecycleManager::class.java)
    }
}
