package org.radarbase.authorizer.inject

import org.radarbase.authorizer.service.RadarProjectService
import org.radarbase.jersey.auth.ProjectService
import javax.inject.Provider
import javax.ws.rs.core.Context

class ProjectServiceWrapper(
    @Context private val mpProjectService: Provider<RadarProjectService>
) : ProjectService {
    override fun ensureProject(projectId: String) = mpProjectService.get().ensureProject(projectId)
}
