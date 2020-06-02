// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  BACKEND_BASE_URL: 'http://localhost:8080',
  VALIDATE: false,
  AUTH_GRANT_TYPE: 'authorization_code',
  AUTH_CLIENT_ID: 'radar_rest_sources_auth',
  AUTH_CLIENT_SECRET: 'secret',
  AUTH_CALLBACK_URL: 'http://localhost:8080/oauth/callback',
  BASE_HREF: '/rest-sources/authorizer/',
  AUTH_URI: 'http://localhost:8080/oauth'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
