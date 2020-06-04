import { ManagementPortalAuthService } from './management-portal-auth.service';
import { SimpleAuthService } from './simple-auth.service.';
import { environment } from 'src/environments/environment';
import { grantType } from '../enums/grant-type';

export function AuthServiceFactory(httpClient, jwtHelper) {
  switch (environment.authorizationGrantType) {
    case grantType.AUTHORIZATION_CODE:
      return new ManagementPortalAuthService(httpClient, jwtHelper);
    default:
      return new SimpleAuthService();
  }
}
