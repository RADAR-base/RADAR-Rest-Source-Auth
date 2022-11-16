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

package org.radarbase.authorizer.resources

import jakarta.annotation.Resource
import jakarta.inject.Singleton
import jakarta.ws.rs.*
import jakarta.ws.rs.core.Context
import jakarta.ws.rs.core.MediaType
import org.radarbase.auth.authorization.Permission
import org.radarbase.authorizer.api.*
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.Authenticated
import org.radarbase.jersey.auth.NeedsPermission
import org.radarbase.jersey.cache.Cache
import org.radarbase.jersey.service.managementportal.RadarProjectService

@Path("projects")
@Authenticated
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Resource
@Singleton
class ProjectResource(
    @Context private val projectService: RadarProjectService,
    @Context private val auth: Auth,
) {

    @GET
    @NeedsPermission(Permission.PROJECT_READ)
    @Cache(maxAge = 300, isPrivate = true)
    fun projects() = ProjectList(
        projectService.userProjects(auth)
            .map { it.toProject() }
    )

    @GET
    @Path("{projectId}/users")
    @NeedsPermission(Permission.SUBJECT_READ, "projectId")
    @Cache(maxAge = 60, isPrivate = true)
    fun users(
        @PathParam("projectId") projectId: String,
    ) = UserList(
        projectService.projectSubjects(projectId)
            .map { it.toUser() }
    )

    @GET
    @Path("{projectId}")
    @NeedsPermission(Permission.PROJECT_READ, "projectId")
    @Cache(maxAge = 300, isPrivate = true)
    fun project(@PathParam("projectId") projectId: String): Project {
        return projectService.project(projectId).toProject()
    }
}
