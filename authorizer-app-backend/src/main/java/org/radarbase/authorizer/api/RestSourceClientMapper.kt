package org.radarbase.authorizer.api

import org.radarbase.authorizer.RestSourceClient

class RestSourceClientMapper {
  fun fromSourceClientConfig(client: RestSourceClient)= ShareableClientDetail(
      clientId = client.clientId,
      sourceType = client.sourceType,
      scope = client.scope,
      authorizationEndpoint = client.authorizationEndpoint,
      tokenEndpoint = client.tokenEndpoint,
      grantType = client.grantType,
  )

  fun fromSourceClientConfigs(clientConfigs: List<RestSourceClient>) = ShareableClientDetails(
      sourceClients = clientConfigs.map(::fromSourceClientConfig)
  )
}
