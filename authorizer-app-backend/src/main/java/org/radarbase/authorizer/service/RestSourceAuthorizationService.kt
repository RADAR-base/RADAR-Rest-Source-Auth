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
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.ws.rs.core.Context

abstract open class RestSourceAuthorizationService(
    @Context private val restSourceClients: RestSourceClients,
    @Context private val httpClient: OkHttpClient,
    @Context private val objectMapper: ObjectMapper
) {

    abstract fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken?

    abstract fun refreshToken(refreshToken: String, sourceType: String): RestOauth2AccessToken?

    abstract fun revokeToken(accessToken: String, sourceType: String): Boolean

    abstract fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String

    companion object {
        val logger: Logger = LoggerFactory.getLogger(RestSourceAuthorizationService::class.java)
    }
}
