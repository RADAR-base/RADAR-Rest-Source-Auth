export const environment = {
  production: true,
  // base url of the rest-sources authorizer app
  BASE_HREF: '/rest-sources/authorizer/',
  // base url of the rest-sources-auth-backend
  BACKEND_BASE_URL: 'http://localhost:8080',
  // If user validation is enabled
  VALIDATE: false,
  // Grant-type of authorization
  AUTH_GRANT_TYPE: 'authorization_code',
  // Client id of the authorizer app.
  AUTH_CLIENT_ID: 'radar_rest_sources_auth',
  // Client secret of the authorizer app.
  AUTH_CLIENT_SECRET: 'secret',
  // Callback URL registered in MP.
  AUTH_CALLBACK_URL: 'http://localhost:8080/oauth/callback',
  // Management Portal URL.
  AUTH_URI: 'http://localhost:8080/oauth'
};
