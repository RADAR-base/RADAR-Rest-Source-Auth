/*
 *
 *  * Copyright 2019 The Hyve
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *   http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *  *
 *
 */

package org.radarbase.authorizer.service.managementportal

import org.radarbase.authorizer.Config
import org.radarbase.authorizer.api.Project
import org.radarbase.authorizer.api.User
import org.radarbase.authorizer.service.RadarProjectService
import org.radarbase.authorizer.util.CachedSet
import org.radarbase.jersey.auth.Auth
import org.radarbase.jersey.exception.HttpNotFoundException
import org.radarcns.auth.authorization.Permission
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.ws.rs.core.Context

class MPProjectService(@Context private val config: Config, @Context private val mpClient: MPClient) : RadarProjectService {
  private val projects = CachedSet(
      Duration.ofMinutes(config.service.syncProjectsIntervalMin),
      Duration.ofMinutes(1)) {
    mpClient.readProjects()
  }

  private val participants: ConcurrentMap<String, CachedSet<User>> = ConcurrentHashMap()

  override fun ensureProject(projectId: String) {
    if (projects.find { it.id == projectId } == null) {
      throw HttpNotFoundException("project_not_found", "Project $projectId not found in Management Portal.")
    }
  }

  override fun userProjects(auth: Auth): List<Project> {
    return projects.get()
        .filter { auth.token.hasPermissionOnProject(Permission.PROJECT_READ, it.id) }
  }

  override fun project(projectId: String): Project = projects.find { it.id == projectId }
      ?: throw HttpNotFoundException("project_not_found", "Project $projectId not found in Management Portal.")

  override fun projectUsers(projectId: String): List<User> {
    val projectParticipants = participants.computeIfAbsent(projectId) {
      CachedSet(Duration.ofMinutes(config.service.syncParticipantsIntervalMin), Duration.ofMinutes(1)) {
        mpClient.readParticipants(projectId)
      }
    }

    return projectParticipants.get().toList()
  }

  override fun userByExternalId(projectId: String, externalUserId: String): User? =
      projectUsers(projectId).find { it.externalId == externalUserId }

}
