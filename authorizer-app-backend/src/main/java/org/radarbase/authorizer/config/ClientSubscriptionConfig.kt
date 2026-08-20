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

/**
 * Configuration of the per-user subscriptions a [RestSourceClient] manages at its source, i.e.
 * telling that source which users this deployment wants data for. Clients that manage no
 * subscriptions leave the `subscription` block out entirely.
 *
 * Google Health is the only source with subscriptions so far, so the fields it needs are the ones
 * that are here; a client sets only what its own source uses and leaves the rest unset.
 */
data class ClientSubscriptionConfig(
    /** Master switch for subscription management, without unsetting the credentials it needs. */
    val enabled: Boolean = true,
    /** Path to the service-account JSON used to call the subscription API. Google Health only. */
    val serviceAccountKeyPath: String? = null,
    /** Base URL of the API that owns the subscriptions. */
    val apiBaseUrl: String = "https://health.googleapis.com/v4",
    /** Cloud project that owns the subscriber. Google Health only. */
    val googleCloudProjectId: String = "",
    /** Subscriber the per-user subscriptions belong to. */
    val subscriberId: String = "radar-pep",
    /** How often the background reconcile compares local state against the source's subscriptions. */
    val reconcileIntervalMinutes: Long = 5,
    /**
     * If a single pass would delete more orphaned subscriptions than this, it skips deletion and
     * warns instead. Zero or less disables the cap.
     */
    val reconcileMaxDeletesPerPass: Int = 50,
    /**
     * Data types each per-user subscription subscribes to. MUST stay identical to the Push Endpoint's
     * `pushIntegration.googlehealth.triggerDataTypes`, which configures the shared subscriber these
     * subscriptions hang off — a type present here but not there never yields a webhook trigger.
     * Only valid trigger types may be listed; anything else makes Google reject the create outright.
     */
    val dataTypes: List<String> = listOf(
        "steps",
        "heart-rate",
        "heart-rate-variability",
        "daily-resting-heart-rate",
        "respiratory-rate-sleep-summary",
        "daily-sleep-temperature-derivations",
        "sleep",
        "exercise",
        "floors",
        "sedentary-period",
        "activity-level",
    ),
) {
    fun withEnv(): ClientSubscriptionConfig =
        copyEnv("GOOGLE_HEALTH_SERVICE_ACCOUNT_PATH") { copy(serviceAccountKeyPath = it) }

    val isConfigured: Boolean
        get() = enabled && !serviceAccountKeyPath.isNullOrEmpty() && googleCloudProjectId.isNotEmpty()
}
