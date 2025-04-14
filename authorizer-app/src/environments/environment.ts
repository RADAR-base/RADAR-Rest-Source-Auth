// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  radarBaseUrl: 'https://radar-k3s-test.thehyve.net',
  production: false,
  baseHref: '/rest-sources/authorizer/',
  // base url of the rest-sources authorizer app
  backendBaseUrl: 'https://radar-k3s-test.thehyve.net/rest-sources/backend',
  // If user validation is enabled
  doValidate: false,
  // Grant-type of authorization
  authorizationGrantType: 'authorization_code',
  // Client id of the authorizer app.
  appClientId: 'radar_rest_sources_authorizer',
  // Client secret of the authorizer app.
  appClientSecret: '',
  // Callback URL registered in MP.
  authCallbackUrl: 'http://localhost:8080/login',
  // Management Portal URL.
  authBaseUrl: 'https://radar-k3s-test.thehyve.net/managementportal/oauth',
  // Appended to the authBaseUrl to construct the full auth URL
  authPath: '/authorize'
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
