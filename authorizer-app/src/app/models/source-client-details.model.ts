export class RestSourceClientDetailsList {
  sourceClients: RestSourceClientDetails[];
}

export class RestSourceClientDetails {
  sourceType?: string;
  authorizationEndpoint?: string;
  clientId?: string;
  scope?: string;
  redirectUrl?: string;
  grantType?: string;
}
