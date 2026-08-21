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

package org.radarbase.authorizer.config

import org.radarbase.jersey.hibernate.config.DatabaseConfig

data class AuthorizerConfig(
    val service: AuthorizerServiceConfig = AuthorizerServiceConfig(),
    val auth: AuthConfig = AuthConfig(),
    val database: DatabaseConfig = DatabaseConfig(),
    val restSourceClients: List<RestSourceClient> = emptyList(),
    val restSourceClientSubscriptions: RestSourceClientSubscriptions = RestSourceClientSubscriptions(),
    val redis: RedisConfig = RedisConfig(),
) {
    /** Applies the environment overrides of every subscription, e.g. a key path kept out of the file. */
    fun withSubscriptionEnv(env: (String?) -> String? = System::getenv): AuthorizerConfig =
        copy(restSourceClientSubscriptions = restSourceClientSubscriptions.withEnv(env))

    /**
     * Google Health subscription configuration, or a disabled one when that source manages no
     * subscriptions, so that callers can read it without a null check.
     */
    val googleHealth: GoogleHealthSubscriptionConfig
        get() = restSourceClientSubscriptions.googleHealth ?: NO_SUBSCRIPTIONS

    companion object {
        private val NO_SUBSCRIPTIONS = GoogleHealthSubscriptionConfig(enabled = false)
    }
}
