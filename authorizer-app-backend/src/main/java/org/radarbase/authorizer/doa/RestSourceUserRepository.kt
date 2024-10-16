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
import java.time.Instant

interface RestSourceUserRepository {
    suspend fun create(user: RestSourceUserDTO): RestSourceUser
    suspend fun updateToken(token: RestOauth2AccessToken?, user: RestSourceUser): RestSourceUser
    suspend fun read(id: Long): RestSourceUser?
    suspend fun update(userId: Long, user: RestSourceUserDTO): RestSourceUser
    suspend fun query(
        page: Page,
        projectIds: List<String>,
        sourceType: String? = null,
        search: String?,
        userIds: List<String>,
        isAuthorized: Boolean?,
    ): Pair<List<RestSourceUser>, Page>
    suspend fun queryAllWithElapsedEndDate(sourceType: String? = null): List<RestSourceUser>
    suspend fun delete(user: RestSourceUser)
    suspend fun reset(user: RestSourceUser, startDate: Instant, endDate: Instant?): RestSourceUser
    suspend fun findByExternalId(externalId: String, sourceType: String): RestSourceUser?
    suspend fun findByUserIdProjectIdSourceType(userId: String, projectId: String, sourceType: String): RestSourceUser?
}
