package org.radarbase.authorizer.doa

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.exception.HttpBadGatewayException
import java.time.Duration
import java.time.Instant
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.ws.rs.core.Context

class RestSourceUserRepositoryImpl(
    @Context private var em: Provider<EntityManager>
) : RestSourceUserRepository {

  override fun createOrUpdate(token: RestOauth2AccessToken, sourceType: String): RestSourceUser = em.get().transact {
    val externalUserId = token.externalUserId ?: throw HttpBadGatewayException("Could not get externalId from token")

    val queryString = "SELECT u FROM RestSourceUser u where u.sourceType = :sourceType AND u.externalUserId = :externalUserId"
    val existingUser = createQuery(queryString, RestSourceUser::class.java)
        .setParameter("sourceType", sourceType)
        .setParameter("externalUserId", externalUserId)
        .resultList.firstOrNull()

    if(existingUser == null) {
      RestSourceUser().apply {
        this.authorized = true
        this.externalUserId = externalUserId
        this.sourceType = sourceType
        this.startDate = Instant.now()
        this.accessToken = token.accessToken
        this.refreshToken = token.refreshToken
        this.expiresIn = token.expiresIn
        this.expiresAt = Instant.now().plusSeconds(token.expiresIn.toLong()).minus(expiryTimeMargin)
      }.also { persist(it) }
    } else {
      existingUser.apply {
        this.accessToken = token.accessToken
        this.refreshToken = token.refreshToken
        this.expiresIn = token.expiresIn
        this.expiresAt = Instant.now().plusSeconds(token.expiresIn.toLong()).minus(expiryTimeMargin)
      }.also { merge(it) }
    }
  }

  override fun read(id: Long): RestSourceUser? = em.get().transact { find(RestSourceUser::class.java, id) }

  override fun update(existingUser: RestSourceUser, user: RestSourceUserDTO): RestSourceUser = em.get().transact {
    existingUser.apply {
      this.projectId = user.projectId
        this.userId = user.userId
        this.sourceId = user.sourceId
        this.startDate = user.startDate
        this.endDate = user.endDate
    }.also { merge(it) }
  }

  override fun query(page: Page, projectId: String?, sourceType: String?): Pair<List<RestSourceUser>, Page> {
    var queryString = "SELECT u FROM RestSourceUser u"
    var countQueryString = "SELECT count(u) FROM RestSourceUser u"

    when {
      projectId != null && sourceType != null -> {
        queryString += " WHERE u.projectId = :projectId AND u.sourceType = :sourceType"
        countQueryString += " WHERE u.projectId = :projectId AND u.sourceType = :sourceType"
      }
      projectId != null && sourceType == null -> {
        queryString += " WHERE u.projectId = :projectId"
        countQueryString += " WHERE u.projectId = :projectId"
      }
      projectId == null && sourceType != null -> {
        queryString += " WHERE u.sourceType = :sourceType"
        countQueryString += " WHERE u.sourceType = :sourceType"
      }
    }

    val actualPage = page.createValid(maximum = 100)
    return em.get().transact {
      val query = createQuery(queryString, RestSourceUser::class.java)
          .setFirstResult(actualPage.offset)
          .setMaxResults(actualPage.pageSize!!)

      val countQuery = createQuery(countQueryString)

      when {
        projectId != null && sourceType != null -> {
          query.setParameter("projectId", projectId)
          countQuery.setParameter("projectId", projectId)
          query.setParameter("sourceType", sourceType)
          countQuery.setParameter("sourceType", sourceType)
        }
        projectId != null && sourceType == null -> {
          query.setParameter("projectId", projectId)
          countQuery.setParameter("projectId", projectId)
        }
        projectId == null && sourceType != null -> {
          query.setParameter("sourceType", sourceType)
          countQuery.setParameter("sourceType", sourceType)
        }
      }

      val users = query.resultList
      val count = countQuery.singleResult as Long

      Pair(users, actualPage.copy(totalElements = count))
    }
  }

  override fun delete(user: RestSourceUser) = em.get().transact {
    remove(merge(user))
  }

  override fun reset(user: RestSourceUser, startDate: Instant, endDate: Instant?) = em.get().transact {
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
