package org.radarbase.authorizer.doa

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.doa.entitiy.RestSourceUser

interface RestSourceUserRepository {

    fun create(user: RestSourceUser): RestSourceUser
    fun read(id: Long): RestSourceUser?
    fun update(user: RestSourceUser): RestSourceUser
    fun query(page: Page, sourceType: String?, externalUserId: String?): Pair<List<RestSourceUser>, Page>
    fun findAllBySourceType(sourceType: String?): List<RestSourceUser>
    fun delete(user: RestSourceUser)
}
