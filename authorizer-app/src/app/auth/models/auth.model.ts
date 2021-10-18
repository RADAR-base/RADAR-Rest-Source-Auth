export interface AuthResponse {
  access_token: string;
  refresh_token: string;
  expires_in: number;
  iat: number;
  iss: string;
  jti: string;
  token_type: string;
  sub: string;
  roles: string[];
}

export interface User {
  username: string;
  name: string;
  roles: string[];
}
