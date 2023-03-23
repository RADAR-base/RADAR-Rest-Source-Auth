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

package org.radarbase.authorizer.enhancer

import jakarta.inject.Singleton
import org.glassfish.jersey.internal.inject.AbstractBinder
import org.radarbase.authorizer.api.RestSourceClientMapper
import org.radarbase.authorizer.api.RestSourceUserMapper
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.config.RestSourceClients
import org.radarbase.authorizer.doa.RegistrationRepository
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.RestSourceUserRepositoryImpl
import org.radarbase.authorizer.service.*
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.FITBIT_AUTH
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GARMIN_AUTH
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer
import org.radarbase.jersey.filter.Filters

class AuthorizerResourceEnhancer(
    private val config: AuthorizerConfig,
) : JerseyResourceEnhancer {
    private val restSourceClients = RestSourceClients(
        config.restSourceClients
            .map { it.withEnv() }
            .onEach {
                requireNotNull(it.clientId) { "Client ID of ${it.sourceType} is missing" }
                requireNotNull(it.clientSecret) { "Client secret of ${it.sourceType} is missing" }
            },
    )

    override val classes: Array<Class<*>>
        get() = listOfNotNull(
            Filters.cache,
            Filters.logResponse,
            if (config.service.enableCors == true) Filters.cors else null,
        ).toTypedArray()

    override val packages: Array<String> = arrayOf(
        "org.radarbase.authorizer.resources",
        "org.radarbase.authorizer.lifecycle",
    )

    override fun AbstractBinder.enhance() {
        // Bind instances. These cannot use any injects themselves
        bind(config)
            .to(AuthorizerConfig::class.java)

        bind(restSourceClients)
            .to(RestSourceClients::class.java)

        bind(RegistrationRepository::class.java)
            .to(RegistrationRepository::class.java)
            .`in`(Singleton::class.java)

        bind(RegistrationService::class.java)
            .to(RegistrationService::class.java)
            .`in`(Singleton::class.java)

        bind(RestSourceUserService::class.java)
            .to(RestSourceUserService::class.java)
            .`in`(Singleton::class.java)

        bind(RestSourceUserMapper::class.java)
            .to(RestSourceUserMapper::class.java)
            .`in`(Singleton::class.java)

        bind(RestSourceClientMapper::class.java)
            .to(RestSourceClientMapper::class.java)
            .`in`(Singleton::class.java)

        bind(RestSourceClientService::class.java)
            .to(RestSourceClientService::class.java)
            .`in`(Singleton::class.java)

        bind(RestSourceUserRepositoryImpl::class.java)
            .to(RestSourceUserRepository::class.java)
            .`in`(Singleton::class.java)

        bind(DelegatedRestSourceAuthorizationService::class.java)
            .to(RestSourceAuthorizationService::class.java)

        bind(GarminSourceAuthorizationService::class.java)
            .to(RestSourceAuthorizationService::class.java)
            .named(GARMIN_AUTH)
            .`in`(Singleton::class.java)

        bind(OAuth2RestSourceAuthorizationService::class.java)
            .to(RestSourceAuthorizationService::class.java)
            .named(FITBIT_AUTH)
            .`in`(Singleton::class.java)
    }
}
