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

import jakarta.inject.Provider
import jakarta.ws.rs.core.Context
import org.hibernate.criterion.MatchMode
import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.exception.HttpConflictException
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarbase.jersey.hibernate.HibernateRepository
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.persistence.EntityManager

class RestSourceUserRepositoryImpl(
    @Context em: Provider<EntityManager>,
) : RestSourceUserRepository, HibernateRepository(em) {

    override fun create(token: RestOauth2AccessToken, sourceType: String): RestSourceUser = transact {
        val externalUserId = token.externalUserId
            ?: throw HttpBadGatewayException("Could not get externalId from token")

        val queryString =
            "SELECT u FROM RestSourceUser u where u.sourceType = :sourceType AND u.externalUserId = :externalUserId"
        val existingUser = createQuery(queryString, RestSourceUser::class.java)
            .setParameter("sourceType", sourceType)
            .setParameter("externalUserId", externalUserId)
            .resultList.firstOrNull()

        when {
            existingUser == null -> RestSourceUser().apply {
                this.externalUserId = externalUserId
                this.sourceType = sourceType
                this.startDate = Instant.now()
                setToken(token)
                persist(this)
            }
            // Do not override existing user, except if it is not fully specified
            existingUser.projectId != null -> throw HttpConflictException(
                "external-id-exists",
                "External-user-id ${existingUser.externalUserId} " +
                    "for source-type ${existingUser.sourceType} is already in use by user ${existingUser.userId}." +
                    " Please remove the existing user to continue or update existing user.",
            )
            else -> existingUser.apply {
                this.startDate = Instant.now()
                setToken(token)
                merge(this)
            }
        }
    }

    private fun RestSourceUser.setToken(token: RestOauth2AccessToken?) {
        if (token != null) {
            this.authorized = true
            this.accessToken = token.accessToken
            this.refreshToken = token.refreshToken
            this.expiresIn = token.expiresIn
            this.expiresAt = Instant.now().plusSeconds(token.expiresIn.toLong()) - expiryTimeMargin
        } else {
            this.authorized = false
            this.accessToken = null
            this.refreshToken = null
            this.expiresIn = null
            this.expiresAt = null
        }
    }

    override fun updateToken(token: RestOauth2AccessToken?, userId: Long): RestSourceUser = transact {
        val existingUser = find(RestSourceUser::class.java, userId)
            ?: throw HttpNotFoundException("user_not_found", "User with ID $userId does not exist")

        existingUser.apply {
            setToken(token)
            merge(this)
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
            this.authorized = user.isAuthorized
            merge(this)
        }
    }

    override fun query(
        page: Page,
        projectIds: List<String>,
        sourceType: String?,
        search: String?,
        userIds: List<String>,
    ): Pair<List<RestSourceUser>, Page> {
        val queryString = "SELECT u FROM RestSourceUser u WHERE u.projectId IN :projectIds"
        val countQueryString = "SELECT count(u) FROM RestSourceUser u WHERE u.projectId IN :projectIds"

        var whereClauses = ""
        if (sourceType != null) {
            whereClauses += " AND u.sourceType = :sourceType"
        }
        if (search != null) {
            whereClauses += "AND (u.userId LIKE :search OR u.userId IN :userIds)"
        }

        val actualPage = page.createValid(maximum = Integer.MAX_VALUE)
        return transact {
            val query = createQuery(queryString + whereClauses, RestSourceUser::class.java)
                .setFirstResult(actualPage.offset)
                .setMaxResults(actualPage.pageSize)

            val countQuery = createQuery(countQueryString + whereClauses)

            query.setParameter("projectIds", projectIds)
            countQuery.setParameter("projectIds", projectIds)

            if (sourceType != null) {
                query.setParameter("sourceType", sourceType)
                countQuery.setParameter("sourceType", sourceType)
            }
            if (search != null) {
                // user IDs are always lower case in MP.
                val searchMatch = MatchMode.ANYWHERE.toMatchString(search.lowercase())
                query.setParameter("search", searchMatch)
                query.setParameter("userIds", userIds)
                countQuery.setParameter("search", searchMatch)
                countQuery.setParameter("userIds", userIds)
            }

            val users = query.resultList
            val count = countQuery.singleResult as Long

            Pair(users, actualPage.copy(totalElements = count))
        }
    }

    override fun queryAllWithElapsedEndDate(
        sourceType: String?,
    ): List<RestSourceUser> {
        var queryString = """
               SELECT u
               FROM RestSourceUser u
               WHERE u.endDate < :prevFourteenDays
        """.trimIndent()

        if (sourceType != null) {
            queryString += " AND u.sourceType = :sourceType"
        }

        return transact {
            val query = createQuery(queryString, RestSourceUser::class.java)
            if (sourceType != null) {
                query.setParameter("sourceType", sourceType)
            }
            query.setParameter("prevFourteenDays", Instant.now().minus(14, ChronoUnit.DAYS))
            query.resultList
        }
    }

    override fun findByExternalId(
        externalId: String,
        sourceType: String,
    ): RestSourceUser? {
        val queryString = """
               SELECT u
               FROM RestSourceUser u
               WHERE u.externalUserId = :externalId
               AND u.sourceType = :sourceType
        """.trimIndent()
        val result = transact {
            val query = createQuery(queryString, RestSourceUser::class.java)
            query.setParameter("sourceType", sourceType)
            query.setParameter("externalId", externalId)
            query.resultList
        }
        return if (result.isEmpty()) null else result.get(0)
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
