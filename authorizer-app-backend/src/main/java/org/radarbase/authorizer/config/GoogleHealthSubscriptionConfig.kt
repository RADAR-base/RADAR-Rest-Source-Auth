/*
 *  Copyright 2026 King's College London
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

package org.radarbase.authorizer.config

import org.radarbase.jersey.config.ConfigLoader.copyEnv

data class GoogleHealthSubscriptionConfig(
    val serviceAccountKeyPath: String? = null,
    val apiBaseUrl: String = "https://health.googleapis.com/v4",
    val googleCloudProjectId: String = "",
    val subscriberId: String = "radar-pep",
    /** Data types each per-user subscription subscribes to  */
    val dataTypes: List<String> = listOf(
        "steps", "heart-rate", "heart-rate-variability", "total-calories",
        "daily-resting-heart-rate", "respiratory-rate-sleep-summary",
        "daily-sleep-temperature-derivations", "sleep", "exercise",
    ),
) {
    fun withEnv(): GoogleHealthSubscriptionConfig =
        copyEnv("GOOGLE_HEALTH_SERVICE_ACCOUNT_PATH") { copy(serviceAccountKeyPath = it) }

    val isConfigured: Boolean
        get() = !serviceAccountKeyPath.isNullOrEmpty() && googleCloudProjectId.isNotEmpty()
}
