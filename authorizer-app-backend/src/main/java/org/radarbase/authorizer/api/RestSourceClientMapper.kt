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

package org.radarbase.authorizer.api

import org.radarbase.authorizer.RestSourceClient

class RestSourceClientMapper {
    fun toSourceClientConfig(client: RestSourceClient) = client.run {
        ShareableClientDetail(
            clientId = requireNotNull(clientId) { "Client ID of sourceType $sourceType not specified" },
            sourceType = sourceType,
            scope = scope,
            preAuthorizationEndpoint = preAuthorizationEndpoint,
            authorizationEndpoint = authorizationEndpoint,
            deregistrationEndpoint = deregistrationEndpoint,
            tokenEndpoint = tokenEndpoint,
            grantType = grantType,
        )
    }

    fun fromSourceClientConfigs(clientConfigs: List<RestSourceClient>) = ShareableClientDetails(
        sourceClients = clientConfigs.map { toSourceClientConfig(it) }
    )
}
