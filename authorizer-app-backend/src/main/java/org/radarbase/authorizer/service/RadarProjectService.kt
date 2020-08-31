package org.radarbase.authorizer.service

import org.radarbase.authorizer.api.Project
import org.radarbase.authorizer.api.User
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.auth.ProjectService


interface RadarProjectService : ProjectService {
    fun project(projectId: String): Project
    fun userProjects(auth: Auth): List<Project>
    fun projectUsers(projectId: String): List<User>
    fun userByExternalId(projectId: String, externalUserId: String): User?
}
