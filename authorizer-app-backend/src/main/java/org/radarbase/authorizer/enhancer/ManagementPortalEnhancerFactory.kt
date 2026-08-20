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

import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.entity.RegistrationState
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.jersey.auth.AuthConfig
import org.radarbase.jersey.auth.MPConfig
import org.radarbase.jersey.enhancer.EnhancerFactory
import org.radarbase.jersey.enhancer.Enhancers
import org.radarbase.jersey.enhancer.JerseyResourceEnhancer
import org.radarbase.jersey.hibernate.config.HibernateResourceEnhancer

/** This binder needs to register all non-Jersey classes, otherwise initialization fails. */
class ManagementPortalEnhancerFactory(
    private val config: AuthorizerConfig,
) : EnhancerFactory {
    override fun createEnhancers(): List<JerseyResourceEnhancer> {
        val authConfig =
            AuthConfig(
                managementPortal =
                MPConfig(
                    url = config.auth.managementPortalUrl.trimEnd('/'),
                    clientId = config.auth.clientId,
                    clientSecret = config.auth.clientSecret,
                    syncProjectsIntervalMin = config.service.syncProjectsIntervalMin,
                    syncParticipantsIntervalMin = config.service.syncParticipantsIntervalMin,
                ),
                jwtResourceName = config.auth.jwtResourceName,
                jwksUrls = config.auth.jwksUrls.ifEmpty {
                    listOf("${config.auth.managementPortalUrl.trimEnd('/')}/oauth/token_key")
                },
            )

        val dbConfig =
            config.database.copy(
                managedClasses =
                listOf(
                    RestSourceUser::class.qualifiedName!!,
                    RegistrationState::class.qualifiedName!!,
                ),
            )
        return listOf(
            Enhancers.radar(authConfig),
            Enhancers.health,
            HibernateResourceEnhancer(dbConfig),
            ManagementPortalResourceEnhancer(authConfig),
            Enhancers.ecdsa,
            JedisResourceEnhancer(),
            Enhancers.exception,
            AuthorizerResourceEnhancer(config),
        )
    }
}
