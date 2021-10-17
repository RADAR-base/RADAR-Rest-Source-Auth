// This file can be replaced during build by using the `fileReplacements` array.
// `ng build --prod` replaces `environment.ts` with `environment.prod.ts`.
// The list of file replacements can be found in `angular.json`.

export const environment = {
  production: false,
  backendBaseUrl: 'https://radar-k3s-test.thehyve.net/rest-sources/backend',
  doValidate: false,
  authorizationGrantType: 'authorization_code',
  appClientId: 'radar_rest_sources_authorizer',
  appClientSecret: '',
  authCallbackUrl: 'http://localhost:8080/login',
  BASE_HREF: '/rest-sources/authorizer/',
  authBaseUrl: 'https://radar-k3s-test.thehyve.net/managementportal/oauth',
};

/*
 * For easier debugging in development mode, you can import the following file
 * to ignore zone related error stack frames such as `zone.run`, `zoneDelegate.invokeTask`.
 *
 * This import should be commented out in production mode because it will have a negative impact
 * on performance if an error is thrown.
 */
// import 'zone.js/dist/zone-error';  // Included with Angular CLI.
