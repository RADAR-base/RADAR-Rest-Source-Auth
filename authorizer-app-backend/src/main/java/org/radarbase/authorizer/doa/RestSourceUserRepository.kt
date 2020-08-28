package org.radarbase.authorizer.doa

import org.radarbase.authorizer.api.Page
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.RestSourceUserDTO
import org.radarbase.authorizer.doa.entity.RestSourceUser

interface RestSourceUserRepository {

    fun create(user: RestOauth2AccessToken, sourceType: String): RestSourceUser
    fun read(id: Long): RestSourceUser?
    fun update(existingUser: RestSourceUser, user: RestSourceUserDTO): RestSourceUser
    fun query(page: Page, sourceType: String? = null, externalUserId: String? = null): Pair<List<RestSourceUser>, Page>
//    fun findByExtenalId(sourceType: String, externalUserId: String) : RestSourceUser?
//    fun findAllBySourceType(sourceType: String?): List<RestSourceUser>
//    fun delete(user: RestSourceUser)
}
