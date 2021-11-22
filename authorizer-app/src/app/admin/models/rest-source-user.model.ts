export interface RestSourceUser {
     id?: string;
     userId: string;
     projectId: string;
     externalId?: string;
     serviceUserId?: string;
     sourceId?: string;
     startDate?: string;
     endDate?: string;
     sourceType: string;
     isAuthorized?: boolean;
     registrationCreatedAt?: string;
     hasValidToken?: boolean;
     timesReset?: number;
     version?: string;
}

export interface RestSourceUsers {
     users: RestSourceUser[];
     metadata: Page;
}

export interface Page {
     pageNumber: number;
     pageSize: number;
     totalElements: number;
}

export interface RestSourceUserRequest {
  projectId: string;
  userId: string;
  sourceId?: string;
  startDate: string;
  endDate?: string;
  sourceType: string;
}

export interface RestSourceUserResponse {
  id: string;
  projectId: string;
  userId: string;
  sourceId: string;
  startDate: string;
  endDate?: string;
  sourceType: string;
  createdAt: string;
  humanReadableUserId: string;
  externalId: string;
  serviceUserId?: string;
  isAuthorized: boolean;
  timesReset: number;
  version: string;
}

export interface RegistrationCreateRequest {
  userId: string; // matches RestSourceUserResponse.id
  persistent?: boolean; // set to true to get a long-living token
}

export interface RegistrationResponse {
  token: string;
  secret?: string; // only defined if the registration is persistent
  userId: string;
  authEndpointUrl?: string; // only defined if the registration is not persistent
  expiresAt: string;
  persistent: boolean;
  project?: any;
  sourceType?: string;
}

export interface RegistrationRequest {
  secret: string;
}

export interface AuthorizeRequest {
  code?: string;
  oauth_token?: string;
  oauth_verifier?: string;
  oauth_token_secret?: string;
}
