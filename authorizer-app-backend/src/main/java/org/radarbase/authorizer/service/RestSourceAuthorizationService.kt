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

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.api.SignRequestParams
import org.radarbase.authorizer.doa.entity.RestSourceUser
import kotlin.time.Duration.Companion.seconds

interface RestSourceAuthorizationService {
    suspend fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken

    suspend fun refreshToken(user: RestSourceUser): RestOauth2AccessToken?

    suspend fun revokeToken(user: RestSourceUser): Boolean

    suspend fun revokeToken(externalId: String, sourceType: String, token: String): Boolean

    suspend fun getAuthorizationEndpointWithParams(
        sourceType: String,
        userId: Long,
        state: String,
    ): String

    fun signRequest(user: RestSourceUser, payload: SignRequestParams): SignRequestParams

    suspend fun deregisterUser(user: RestSourceUser)

    companion object {
        fun httpClient(builder: HttpClientConfig<CIOEngineConfig>.() -> Unit = {}) =
            HttpClient(CIO) {
                install(HttpTimeout) {
                    val millis = 10.seconds.inWholeMilliseconds
                    connectTimeoutMillis = millis
                    socketTimeoutMillis = millis
                    requestTimeoutMillis = millis
                }
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                        },
                    )
                }
                builder()
            }
    }
}
