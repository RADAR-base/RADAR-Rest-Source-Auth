package org.radarbase.authorizer.resources

import org.radarbase.authorizer.api.Project
import org.radarbase.authorizer.api.ProjectList
import org.radarbase.authorizer.api.UserList
import org.radarbase.authorizer.service.RadarProjectService
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarcns.auth.authorization.Permission
import javax.annotation.Resource
import javax.inject.Singleton
import javax.ws.rs.*
import javax.ws.rs.core.Context
import javax.ws.rs.core.MediaType

@Path("projects")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class ProjectResource(
    @Context private val projectService: RadarProjectService,
    @Context private val auth: Auth
) {

    @GET
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ)
    fun projects() = ProjectList(projectService.userProjects(auth))

    @GET
    @Path("{projectId}/users")
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun users(@PathParam("projectId") projectId: String): UserList {
        return UserList(projectService.projectUsers(projectId))
    }

    @GET
    @Path("{projectId}")
    @NeedsPermission(Permission.Entity.PROJECT, Permission.Operation.READ, "projectId")
    fun project(@PathParam("projectId") projectId: String): Project {
        return projectService.project(projectId)
    }
}
