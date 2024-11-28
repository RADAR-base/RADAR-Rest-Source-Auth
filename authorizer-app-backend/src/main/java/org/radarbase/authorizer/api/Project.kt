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

import org.radarbase.management.client.MPProject
import org.radarbase.management.client.MPSubject

data class ProjectList(val projects: List<Project>)

data class Project(
    val id: String,
    val name: String? = null,
    val location: String? = null,
    val organization: String? = null,
    val description: String? = null,
)

fun MPProject.toProject() = Project(
    id = id,
    name = name,
    location = location,
    organization = organization?.id,
    description = description,
)

data class UserList(val users: List<User>)

data class User(val id: String, val projectId: String, val externalId: String? = null, val status: String)

fun MPSubject.toUser() = User(
    id = checkNotNull(id) { "User must have a login" },
    projectId = checkNotNull(projectId) { "User must have a project ID" },
    externalId = externalId,
    status = status,
)
