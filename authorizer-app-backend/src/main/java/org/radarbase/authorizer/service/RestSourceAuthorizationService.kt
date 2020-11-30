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

import org.radarbase.authorizer.api.RequestTokenPayload
import org.radarbase.authorizer.api.RestOauth2AccessToken
import org.radarbase.authorizer.doa.entity.RestSourceUser

interface RestSourceAuthorizationService {

    fun requestAccessToken(payload: RequestTokenPayload, sourceType: String): RestOauth2AccessToken?

    fun refreshToken(user: RestSourceUser): RestOauth2AccessToken?

    fun revokeToken(user: RestSourceUser): Boolean

    fun deRegisterUser(user: RestSourceUser): RestSourceUser

    fun getAuthorizationEndpointWithParams(sourceType: String, callBackUrl: String): String

}
