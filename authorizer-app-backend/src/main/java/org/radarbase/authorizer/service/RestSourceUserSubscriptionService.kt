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

package org.radarbase.authorizer.service

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RestSourceUserSubscriptionRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.model.SubscriptionResult
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GOOGLE_AUTH
import org.slf4j.LoggerFactory
import kotlin.time.Duration.Companion.seconds

class RestSourceUserSubscriptionService(
    @param:Context private val client: GoogleHealthSubscriptionClient,
    @param:Context private val subscriptionRepository: RestSourceUserSubscriptionRepository,
    @param:Context private val lockService: LockService,
    @param:Context private val config: AuthorizerConfig,
) {
    private fun applies(user: RestSourceUser): Boolean =
        config.googleHealth.isConfigured && client.isConfigured && user.sourceType == GOOGLE_AUTH

    private fun isSubscribed(user: RestSourceUser): Boolean = user.subscription?.isSubscribed == true

    private suspend fun <T> withLock(user: RestSourceUser, block: suspend () -> T): T? {
        val userId = user.id ?: return null
        return lockService.runLocked("subscription-$userId", LOCK_TIMEOUT) { block() }
    }

    /** Subscribe or unsubscribe to match the user's current authorization. Called after a token update. */
    suspend fun syncForToken(user: RestSourceUser) {
        if (!applies(user)) return
        val desired = user.authorized && user.accessToken != null
        try {
            withLock(user) {
                when {
                    desired && !isSubscribed(user) -> doSubscribe(user)
                    !desired && isSubscribed(user) -> doUnsubscribe(user)
                    else -> true
                }
            }
        } catch (ex: Exception) {
            logger.warn("Subscription sync failed for user {} — leaving state unchanged", user.userId, ex)
        }
    }

    /** Remove the user's subscription at Google just before the user row is deleted. Best-effort. */
    suspend fun removeForUser(user: RestSourceUser) {
        if (!applies(user)) return
        val healthUserId = user.externalUserId ?: user.subscription?.externalUserId ?: return
        try {
            withLock(user) {
                when (val result = client.deleteSubscription(healthUserId)) {
                    is SubscriptionResult.Success ->
                        logger.info("Deleted subscription for user={} ahead of deregistration", user.userId)
                    else ->
                        logger.warn("Could not delete subscription for user={} ahead of deregistration: {}", user.userId, result)
                }
            }
        } catch (ex: Exception) {
            logger.warn("Subscription removal failed for user {} ahead of deregistration", user.userId, ex)
        }
    }

    /** Force-create the subscription (manual endpoint / reconcile). Returns true when Google confirms it. */
    suspend fun subscribe(user: RestSourceUser): Boolean {
        if (!applies(user)) return false
        return withLock(user) { doSubscribe(user) } ?: false
    }

    /** Force-delete the subscription (manual endpoint / revoke / refresh failure). Returns true when Google confirms it. */
    suspend fun unsubscribe(user: RestSourceUser): Boolean {
        if (!applies(user)) return false
        return withLock(user) { doUnsubscribe(user) } ?: false
    }

    private suspend fun doSubscribe(user: RestSourceUser): Boolean {
        val userId = user.id ?: return false
        val healthUserId = user.externalUserId
        if (healthUserId == null) {
            logger.warn("Cannot subscribe user {} without a Google health user id (externalUserId)", user.userId)
            return false
        }
        return when (val result = client.createSubscription(healthUserId)) {
            is SubscriptionResult.Success -> {
                subscriptionRepository.markSubscribed(userId, healthUserId, true)
                logger.info("Subscribed user={}", user.userId)
                true
            }
            else -> {
                logger.warn("Subscribe failed for user={}: {}", user.userId, result)
                false
            }
        }
    }

    private suspend fun doUnsubscribe(user: RestSourceUser): Boolean {
        val userId = user.id ?: return false
        val healthUserId = user.externalUserId ?: user.subscription?.externalUserId
        if (healthUserId == null) {
            // Nothing exists at Google; just make sure any local flag is cleared.
            if (isSubscribed(user)) subscriptionRepository.markSubscribed(userId, null, false)
            return true
        }
        return when (val result = client.deleteSubscription(healthUserId)) {
            is SubscriptionResult.Success -> {
                subscriptionRepository.markSubscribed(userId, healthUserId, false)
                logger.info("Unsubscribed user={}", user.userId)
                true
            }
            else -> {
                logger.warn("Unsubscribe failed for user={}: {}", user.userId, result)
                false
            }
        }
    }

    companion object {
        private val LOCK_TIMEOUT = 30.seconds
        private val logger = LoggerFactory.getLogger(RestSourceUserSubscriptionService::class.java)
    }
}
