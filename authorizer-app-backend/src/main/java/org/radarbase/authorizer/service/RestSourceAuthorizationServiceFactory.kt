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

import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.util.StateStore
import javax.ws.rs.core.Context

class RestSourceAuthorizationServiceFactory(
        @Context private val restSourceClients: RestSourceClients,
        @Context private val httpClient: OkHttpClient,
        @Context private val objectMapper: ObjectMapper,
        @Context private val stateStore: StateStore
        ) {

    fun getAuthorizationService(sourceType: String): RestSourceAuthorizationService {
        when(sourceType) {
            "Garmin" -> return OAuth1RestSourceAuthorizationService(this.restSourceClients, this.httpClient, this.objectMapper)
            "FitBit" -> return OAuth2RestSourceAuthorizationService(this.restSourceClients, this.httpClient, this.objectMapper, this.stateStore)
            else -> return OAuth2RestSourceAuthorizationService(this.restSourceClients, this.httpClient, this.objectMapper, this.stateStore)
        }
    }

}
