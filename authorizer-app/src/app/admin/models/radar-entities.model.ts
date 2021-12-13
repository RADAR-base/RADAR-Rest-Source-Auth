export interface RadarProject {
     id?: string;
     version?: string;
     description ?: string;
     location ?: string;
}

export interface RadarSubject {
  id?: string;
  projectId?: string;
  externalId?: string;
  status?: string;
  humanReadableUserId?: string;
}

export interface RadarSourceClient {
  authorizationEndpoint: string;
  clientId: string;
  scope: string;
  sourceType: string;
  tokenEndpoint: string;
}
