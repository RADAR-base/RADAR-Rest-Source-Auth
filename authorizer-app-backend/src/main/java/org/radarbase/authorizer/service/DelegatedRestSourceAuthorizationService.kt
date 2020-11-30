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
import org.glassfish.hk2.api.IterableProvider
import org.radarbase.authorizer.RestSourceClients
import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.doa.entity.RestSourceUser
import org.radarbase.authorizer.util.StateStore
import javax.ws.rs.core.Context

class DelegatedRestSourceAuthorizationService(
        @Context private val namedServices: IterableProvider<RestSourceAuthorizationService>
): RestSourceAuthorizationService {

    fun delegate(sourceType: String): RestSourceAuthorizationService {
        return when (sourceType) {
            GARMIN_AUTH -> namedServices.named(GARMIN_AUTH).get()
            FITBIT_AUTH -> namedServices.named(FITBIT_AUTH).get()
            else -> throw IllegalStateException()
        }
    }

    override fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken? =
         delegate(sourceType).requestAccessToken(payload, sourceType)

    override fun refreshToken(user: RestSourceUser): RestOauth2AccessToken? =
            delegate(user.sourceType).refreshToken(user)

    override fun revokeToken(user: RestSourceUser): Boolean =
            delegate(user.sourceType).revokeToken(user)

    override fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String =
            delegate(sourceType).getAuthorizationEndpointWithParams(sourceType, callBackUrl)

    companion object {
        const val GARMIN_AUTH = "Garmin"
        const val FITBIT_AUTH = "FitBit"
    }

}
