export class RestSourceUser {
  id?: string;
  version?: string;
  projectId?: string;
  userId?: string;
  humanReadableUserId?: string;
  serviceUserId?: string;
  externalId?: string;
  sourceId?: string;
  startDate?: string;
  endDate?: string;
  externalUserId?: string;
  sourceType?: string;
  isAuthorized?: boolean;
  hasValidToken?: boolean;
  timesReset?: number;
}

export class RestSourceUsers {
  users: RestSourceUser[];
  metadata: Page;
}

export class Page {
  pageNumber: number;
  pageSize: number;
  totalElements: number;
}

export interface RestSourceUserRequest {
  projectId: string;
  userId: string;
  sourceId?: string;
  startDate: string; // Instant;
  endDate?: string; // Instant;
  sourceType: string;
}

export interface RestSourceUserResponse {
  id: string;
  projectId: string;
  userId: string;
  sourceId: string;
  startDate: string; // Instant
  endDate?: string; // Instant
  sourceType: string;
  createdAt: string; // Instant
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
  expiresAt: string; // Instant
  persistent: boolean;
}
// interface RegistrationResponse {
//   token: String
//   userId: String
//   authEndpointUrl: String
//   expiresAt: Instant
//   persistent: Boolean
// }

export interface RegistrationRequest {
  secret: string;
}

export interface AuthorizeRequest {
  code?: string;
  oauth_token?: string;
  oauth_verifier?: string;
  oauth_token_secret?: string;
}
