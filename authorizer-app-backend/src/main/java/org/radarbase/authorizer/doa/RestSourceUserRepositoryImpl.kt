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

package org.radarbase.authorizer.doa

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.hibernate.HibernateRepository
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.exception.HttpNotFoundException
import java.time.Duration
import java.time.Instant
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.ws.rs.core.Context

class RestSourceUserRepositoryImpl(
    @Context em: Provider<EntityManager>
) : RestSourceUserRepository, HibernateRepository(em) {

    override fun create(token: RestOauth2AccessToken, sourceType: String): RestSourceUser = transact {
        val externalUserId = token.externalUserId
            ?: throw HttpBadGatewayException("Could not get externalId from token")

        val queryString = "SELECT u FROM RestSourceUser u where u.sourceType = :sourceType AND u.externalUserId = :externalUserId"
        val existingUser = createQuery(queryString, RestSourceUser::class.java)
            .setParameter("sourceType", sourceType)
            .setParameter("externalUserId", externalUserId)
            .resultList.firstOrNull()

        if (existingUser != null) {
            throw HttpConflictException("external-id-exists", "External-user-id ${existingUser.externalUserId} " +
                    "for source-type ${existingUser.sourceType} is already in use by user ${existingUser.userId}." +
                    " Please remove the existing user to continue or update existing user.")
        }

        RestSourceUser().apply {
            this.authorized = true
            this.externalUserId = externalUserId
            this.sourceType = sourceType
            this.startDate = Instant.now()
            this.accessToken = token.accessToken
            this.refreshToken = token.refreshToken
            this.expiresIn = token.expiresIn
            this.expiresAt = startDate.plusSeconds(token.expiresIn.toLong()).minus(expiryTimeMargin)
            persist(this)
        }
    }

    override fun updateToken(token: RestOauth2AccessToken?, userId: Long): RestSourceUser = transact {
        val existingUser = find(RestSourceUser::class.java, userId)
                ?: throw HttpNotFoundException("user_not_found", "User with ID $userId does not exist")

        if (token == null) {
            existingUser.apply {
                this.authorized = false
                this.accessToken = null
                this.refreshToken = null
                this.expiresIn = null
                this.expiresAt = null
                merge(this)
            }
        } else {
            existingUser.apply {
                this.authorized = true
                this.accessToken = token.accessToken
                this.refreshToken = token.refreshToken
                this.expiresIn = token.expiresIn
                this.expiresAt = Instant.now().plusSeconds(token.expiresIn.toLong()).minus(expiryTimeMargin)
                merge(this)
            }
        }
    }

    override fun read(id: Long): RestSourceUser? = transact { find(RestSourceUser::class.java, id) }

    override fun update(existingUser: RestSourceUser, user: RestSourceUserDTO): RestSourceUser = transact {
        existingUser.apply {
            this.projectId = user.projectId
            this.userId = user.userId
            this.sourceId = user.sourceId
            this.startDate = user.startDate
            this.endDate = user.endDate
            merge(this)
        }
    }

    override fun query(
            page: Page,
            projects: List<String>,
            sourceType: String?
    ): Pair<List<RestSourceUser>, Page> {
        var queryString = "SELECT u FROM RestSourceUser u WHERE u.projectId IN (:projects)"
        var countQueryString = "SELECT count(u) FROM RestSourceUser u WHERE u.projectId IN (:projects)"

        if (sourceType != null) {
            queryString += " AND u.sourceType = :sourceType"
            countQueryString += " AND u.sourceType = :sourceType"
        }

        val actualPage = page.createValid(maximum = Integer.MAX_VALUE)
        return transact {
            val query = createQuery(queryString, RestSourceUser::class.java)
                .setFirstResult(actualPage.offset)
                .setMaxResults(actualPage.pageSize!!)

            val countQuery = createQuery(countQueryString)

            query.setParameter("projects", projects)
            countQuery.setParameter("projects", projects)

            if (sourceType != null) {
                query.setParameter("sourceType", sourceType)
                countQuery.setParameter("sourceType", sourceType)
            }

            val users = query.resultList
            val count = countQuery.singleResult as Long

            Pair(users, actualPage.copy(totalElements = count))
        }
    }

    override fun delete(user: RestSourceUser) = transact {
        remove(merge(user))
    }

    override fun reset(user: RestSourceUser, startDate: Instant, endDate: Instant?) = transact {
        user.apply {
            this.version = Instant.now().toString()
            this.timesReset += 1
            this.startDate = startDate
            this.endDate = endDate
        }.also { merge(it) }
    }

    companion object {
        private val expiryTimeMargin = Duration.ofMinutes(5)
    }
}
