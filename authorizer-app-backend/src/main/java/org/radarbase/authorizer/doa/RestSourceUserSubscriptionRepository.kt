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

package org.radarbase.authorizer.doa

import jakarta.inject.Provider
import jakarta.persistence.EntityManager
import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.doa.entity.RestSourceUserSubscription
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.hibernate.HibernateRepository
import org.radarbase.jersey.service.AsyncCoroutineService

class RestSourceUserSubscriptionRepository(
    @Context em: Provider<EntityManager>,
    @Context asyncService: AsyncCoroutineService,
) : HibernateRepository(em, asyncService) {

    suspend fun findByUserId(userId: Long): RestSourceUserSubscription? = transact {
        createQuery(
            """
            SELECT s
            FROM RestSourceUserSubscription s
            WHERE s.user.id = :userId
            """.trimIndent(),
            RestSourceUserSubscription::class.java,
        ).setParameter("userId", userId).resultList.firstOrNull()
    }

    /**
     * Records the subscription state for a user, creating the row on first use. The subscription's
     * [externalUserId][RestSourceUserSubscription.externalUserId] is updated when a non-null value is
     * supplied, so it always reflects the latest Google health user id.
     */
    suspend fun markSubscribed(
        userId: Long,
        externalUserId: String?,
        subscribed: Boolean,
    ): RestSourceUserSubscription = transact {
        val user = find(RestSourceUser::class.java, userId)
            ?: throw HttpNotFoundException("user_not_found", "Rest-Source-User with ID $userId does not exist")
        val existing = user.subscription
        if (existing == null) {
            RestSourceUserSubscription(
                user = user,
                sourceType = user.sourceType,
                externalUserId = externalUserId,
                isSubscribed = subscribed,
            ).also {
                persist(it)
                user.subscription = it
            }
        } else {
            existing.isSubscribed = subscribed
            if (externalUserId != null) existing.externalUserId = externalUserId
            merge(existing)
        }
    }
}
