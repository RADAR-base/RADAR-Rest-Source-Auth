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

package org.radarbase.authorizer.service

import io.ktor.client.call.body
import io.ktor.client.statement.request
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import jakarta.ws.rs.core.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.glassfish.jersey.server.BackgroundScheduler
import org.radarbase.authorizer.api.RestOauth1AccessToken
import org.radarbase.authorizer.api.RestOauth1UserId
import org.radarbase.authorizer.config.AuthorizerConfig
import org.radarbase.authorizer.doa.RestSourceUserRepository
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.service.DelegatedRestSourceAuthorizationService.Companion.GARMIN_AUTH
import org.radarbase.jersey.exception.HttpBadGatewayException
import org.radarbase.jersey.service.AsyncCoroutineService
import org.radarbase.kotlin.coroutines.forkJoin
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class GarminSourceAuthorizationService(
    @Context private val clientService: RestSourceClientService,
    @Context private val userRepository: RestSourceUserRepository,
    @Context private val asyncService: AsyncCoroutineService,
    @Context
    @BackgroundScheduler
    private val scheduler: ScheduledExecutorService,
    @Context private val config: AuthorizerConfig,
) : OAuth1RestSourceAuthorizationService(
    clientService,
    userRepository,
    config,
) {
    init {
        // This schedules a task that periodically checks users with elapsed end dates and deregisters them.
        scheduler.scheduleAtFixedRate(
            ::checkForUsersWithElapsedEndDateAndDeregister,
            0,
            DEREGISTER_CHECK_PERIOD,
            TimeUnit.MILLISECONDS,
        )
    }

    override suspend fun deregisterUser(user: RestSourceUser) {
        userRepository.delete(user)
    }

    override suspend fun RestOauth1AccessToken.getExternalId(sourceType: String): String = withContext(Dispatchers.IO) {
        // Garmin does not provide the service/external id with the token payload, so an additional
        // request to pull the external id is needed.
        val response = request(HttpMethod.Get, GARMIN_USER_ID_ENDPOINT, this@getExternalId, sourceType)
        when (response.status) {
            HttpStatusCode.OK -> withContext(Dispatchers.IO) {
                response.body<RestOauth1UserId>().userId
            }
            HttpStatusCode.BadRequest, HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden -> throw HttpBadGatewayException("Service was unable to fetch the external ID")
            else -> throw HttpBadGatewayException("Cannot connect to ${response.request.url}: HTTP status ${response.status}")
        }
    }

    private fun checkForUsersWithElapsedEndDateAndDeregister() {
        asyncService.runBlocking {
            userRepository
                .queryAllWithElapsedEndDate(GARMIN_AUTH)
                .forkJoin { revokeToken(it) }
        }
    }

    companion object {
        private const val GARMIN_USER_ID_ENDPOINT = "https://healthapi.garmin.com/wellness-api/rest/user/id"
        private const val DEREGISTER_CHECK_PERIOD = 3600000L
    }
}
