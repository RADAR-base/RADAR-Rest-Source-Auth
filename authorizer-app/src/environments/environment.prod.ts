export const environment = {
  production: true,
  BACKEND_BASE_URL: '/rest-sources/backend',
  VALIDATE: true,
  AUTH: {
    grant_type: 'authorization_code',
    client_id: 'radar_rest_sources_auth',
    client_secret: 'secret',
    scope:
      'SOURCETYPE.READ PROJECT.READ SOURCE.READ SUBJECT.READ MEASUREMENT.READ'
  },
  AUTH_URI: 'http://localhost:8080/oauth'
};
