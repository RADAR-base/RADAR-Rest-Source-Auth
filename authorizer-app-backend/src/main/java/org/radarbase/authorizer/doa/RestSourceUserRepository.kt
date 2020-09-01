package org.radarbase.authorizer.doa

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.doa.entity.RestSourceUser
import java.time.Instant

interface RestSourceUserRepository {
    fun createOrUpdate(user: RestOauth2AccessToken, sourceType: String): RestSourceUser
    fun read(id: Long): RestSourceUser?
    fun update(existingUser: RestSourceUser, user: RestSourceUserDTO): RestSourceUser
    fun query(page: Page, projectId: String? = null, sourceType: String? = null): Pair<List<RestSourceUser>, Page>
    fun delete(user: RestSourceUser)
    fun reset(user: RestSourceUser, startDate: Instant, endDate: Instant?): RestSourceUser
}
