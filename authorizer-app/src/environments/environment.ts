// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  radarBaseUrl: 'https://radar-k3s-test.thehyve.net',
  production: false,
  backendBaseUrl: 'https://radar-k3s-test.thehyve.net/rest-sources/backend', // 'http://localhost:8080',
  // backendBaseUrl: 'https://radar-test.thehyve.net/rest-sources/backend', // 'http://localhost:8080',
  doValidate: false,
  authorizationGrantType: 'authorization_code',
  // appClientId: 'radar_rest_sources_auth',
  appClientId: 'radar_rest_sources_authorizer',
  // appClientId: 'appconfig_frontend',
  appClientSecret: '',
  // appClientSecret: '',
  authCallbackUrl: 'http://localhost:8080/login', //https://radar-k3s-test.thehyve.net/rest-sources/authorizer/login', //'http://localhost:8080/oauth/callback',
  BASE_HREF: '/rest-sources/authorizer/',
  // authBaseUrl: 'https://radar-k3s-test.thehyve.net/managementportal/oauth',
  // authBaseUrl: 'https://radar-test.thehyve.net/managementportal/oauth',
  authBaseUrl: 'https://radar-k3s-test.thehyve.net/managementportal/oauth',
  // authBaseUrl: 'http://localhost:8080/oauth'
};

  // appClientId: 'radar_rest_sources_authorizer',
  // appClientSecret: '', // 'secret',
  // authCallbackUrl: 'https://more-dev.thehyve.net/rest-sources/authorizer/login', // http://localhost:8080/oauth/callback',

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
