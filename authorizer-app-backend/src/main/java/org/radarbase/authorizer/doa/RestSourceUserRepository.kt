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
    fun create(token: RestOauth2AccessToken, sourceType: String): RestSourceUser
    fun updateToken(token: RestOauth2AccessToken?, userId: Long): RestSourceUser
    fun read(id: Long): RestSourceUser?
    fun update(existingUser: RestSourceUser, user: RestSourceUserDTO): RestSourceUser
    fun query(page: Page, projects: List<String>, sourceType: String? = null): Pair<List<RestSourceUser>, Page>
    fun queryAllWithElapsedEndDate(sourceType: String? = null): List<RestSourceUser>
    fun delete(user: RestSourceUser)
    fun reset(user: RestSourceUser, startDate: Instant, endDate: Instant?): RestSourceUser
}
