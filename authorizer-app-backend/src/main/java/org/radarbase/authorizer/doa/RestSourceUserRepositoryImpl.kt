package org.radarbase.authorizer.doa

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.doa.entity.RestSourceUser
import javax.inject.Provider
import javax.persistence.EntityManager
import javax.ws.rs.core.Context

class RestSourceUserRepositoryImpl(
    @Context private var em: Provider<EntityManager>
) : RestSourceUserRepository {
//  override fun create(user: RestSourceUser): RestSourceUser {
//    TODO("Not yet implemented")
//  }

  override fun query(page: Page, sourceType: String?, externalUserId: String?): Pair<List<RestSourceUser>, Page> {
    var queryString = "SELECT u FROM RestSourceUser u"
    var countQueryString = "SELECT count(u) FROM RestSourceUser u"


    val actualPage = page.createValid(maximum = 100)
    return em.get().transact {
      val query = createQuery(queryString, RestSourceUser::class.java)
//          .setParameter("projectId", projectId)
          .setFirstResult(actualPage.offset)
          .setMaxResults(actualPage.pageSize!!)

      val countQuery = createQuery(countQueryString)
//          .setParameter("projectId", projectId)

//      userId?.let {
//        query.setParameter("userId", it)
//        countQuery.setParameter("userId", it)
//      }
//      status?.let {
//        query.setParameter("status", RecordStatus.valueOf(it))
//        countQuery.setParameter("status", RecordStatus.valueOf(it))
//      }
//      sourceType?.let {
//        query.setParameter("sourceType", it)
//        countQuery.setParameter("sourceType", it)
//      }
      val records = query.resultList
      val count = countQuery.singleResult as Long

      Pair(records, actualPage.copy(totalElements = count))
    }
  }

}
