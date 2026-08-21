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

import com.fasterxml.jackson.annotation.JsonProperty
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GOOGLE_AUTH

/**
 * Per-user subscriptions to manage, i.e. telling a source which users this deployment wants data
 * for. One entry per source that has them, named after the `sourceType` of the [RestSourceClient] it
 * belongs to; each source keeps its own settings in its own type, so adding one is a new class and a
 * new field here.
 */
data class RestSourceClientSubscriptions(
    @param:JsonProperty(GOOGLE_AUTH)
    val googleHealth: GoogleHealthSubscriptionConfig? = null,
) {
    /** Applies environment overrides, for values a deployment keeps out of the config file. */
    fun withEnv(env: (String?) -> String? = System::getenv): RestSourceClientSubscriptions =
        copy(googleHealth = googleHealth?.withEnv(env))
}

/** What every source's subscription configuration has in common. */
interface ClientSubscriptionConfig {
    /** Master switch, to stop managing subscriptions without unsetting the credentials they need. */
    val enabled: Boolean

    /** Base URL of the API that owns the subscriptions. */
    val apiBaseUrl: String

    /** Whether subscriptions are [enabled] and everything needed to manage them is configured. */
    val isConfigured: Boolean
}
