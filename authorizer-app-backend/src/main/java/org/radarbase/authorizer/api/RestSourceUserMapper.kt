package org.radarbase.authorizer.api

import org.radarbase.authorizer.doa.entitiy.RestSourceUser


interface RestSourceUserMapper {
    fun fromRestSourceUser(user: RestSourceUser) = RestSourceUserDTO(
            id = user.id.toString(),
            projectId = user.projectId,
            userId = user.userId,
            sourceId = user.sourceId,
            isAuthorized = user.authorized,
            sourceType = user.sourceType,
            endDate = user.endDate,
            startDate = user.startDate,
            externalUserId = user.externalUserId,
            version = user.version,
            timesReset = user.timesReset
    )
}
