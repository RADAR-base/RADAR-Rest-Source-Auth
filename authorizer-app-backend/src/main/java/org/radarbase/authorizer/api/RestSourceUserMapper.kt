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

package org.radarbase.authorizer.api

import org.radarbase.authorizer.doa.entity.RestSourceUser


class RestSourceUserMapper {
    fun fromEntity(user: RestSourceUser) = RestSourceUserDTO(
        id = user.id.toString(),
        projectId = user.projectId,
        userId = user.userId,
        sourceId = user.sourceId,
        isAuthorized = user.hasValidToken(),
        sourceType = user.sourceType,
        endDate = user.endDate,
        startDate = user.startDate,
        externalUserId = user.externalUserId,
        version = user.version,
        timesReset = user.timesReset
    )

    fun fromRestSourceUsers(records: List<RestSourceUser>, page: Page?) = RestSourceUsers(
        users = records.map(::fromEntity)
    )

}
