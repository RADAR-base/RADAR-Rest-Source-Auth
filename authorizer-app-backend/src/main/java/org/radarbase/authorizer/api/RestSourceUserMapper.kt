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

import jakarta.ws.rs.core.Context
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.ProjectService
import org.radarbase.authorizer.service.MPClient
import org.radarbase.kotlin.coroutines.forkJoin
import org.radarbase.jersey.auth.AuthService

class RestSourceUserMapper(
    @Context private val authService: AuthService,
) {
    private val projectService: ProjectService = ProjectService(MPClient(), authService)

    suspend fun fromEntity(user: RestSourceUser): RestSourceUserDTO {
        val mpUser = user.projectId?.let { p ->
            user.userId?.let { u -> projectService.subject(p, u) }
        }
        return RestSourceUserDTO(
            id = user.id.toString(),
            createdAt = user.createdAt,
            projectId = user.projectId,
            userId = user.userId,
            humanReadableUserId = mpUser?.attributes?.get("Human-readable-identifier")
                ?.takeIf { it.isNotBlank() && it != "null" },
            externalId = mpUser?.externalId,
            sourceId = user.sourceId,
            isAuthorized = user.authorized,
            registrationCreatedAt = user.registrations.maxOfOrNull { it.createdAt },
            hasValidToken = user.hasValidToken(),
            sourceType = user.sourceType,
            endDate = user.endDate,
            startDate = user.startDate,
            serviceUserId = user.externalUserId,
            version = user.version,
            timesReset = user.timesReset,
        )
    }

    suspend fun fromRestSourceUsers(records: List<RestSourceUser>, page: Page?) = RestSourceUsers(
        users = records.forkJoin { fromEntity(it) },
        metadata = page,
    )
}
