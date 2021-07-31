import { AuthResponse, User } from '../models/auth.model';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

import { AuthService } from './auth.service';
import { Injectable } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { map } from 'rxjs/operators';

@Injectable()
export class ManagementPortalAuthService extends AuthService {
  DefaultRequestContentType = 'application/x-www-form-urlencoded';
  TOKEN_URI = `${environment.authBaseUrl}/token`;
  // TOKEN_URI = 'http://localhost:8080/managementportal/oauth/token';

  constructor(private http: HttpClient, private jwtHelper: JwtHelperService) {
    super();
  }
  authenticate(authCode) {
    return this.requestAccessToken(authCode).pipe(
      map(res => {
        this.setAccessToken(res.access_token);
        // this.setAccessToken('eyJ0eXAiOiJKV1QiLCJhbGciOiJFUzI1NiJ9.eyJhdWQiOiJyZXNfcmVzdEF1dGhvcml6ZXIiLCJzdWIiOiJwZXltYW4iLCJzb3VyY2VzIjpbXSwiZ3JhbnRfdHlwZSI6ImF1dGhvcml6YXRpb25fY29kZSIsInVzZXJfbmFtZSI6InBleW1hbiIsInJvbGVzIjpbInBsYXlncm91bmQ6Uk9MRV9QUk9KRUNUX0FETUlOIiwicmFkYXI6Uk9MRV9QUk9KRUNUX0FETUlOIl0sInNjb3BlIjpbIlNPVVJDRVRZUEUuUkVBRCIsIlNVQkpFQ1QuVVBEQVRFIiwiUFJPSkVDVC5SRUFEIiwiU1VCSkVDVC5DUkVBVEUiLCJTVUJKRUNULlJFQUQiXSwiaXNzIjoiTWFuYWdlbWVudFBvcnRhbCIsImV4cCI6MTYyNzQ3NzYyMCwiaWF0IjoxNjI3NDc2NzIwLCJhdXRob3JpdGllcyI6WyJST0xFX1BST0pFQ1RfQURNSU4iXSwiY2xpZW50X2lkIjoicmFkYXJfcmVzdF9zb3VyY2VzX2F1dGhvcml6ZXIifQ.eZSnUnlHJ19p0xrtTVgGfCraR2XQ7Bl0ok2HwaWEyMOJHJ1TPBcaRDD_AIy22-23hGO0H1zeJA7NLkBIPJ3PcA');
        this.setUser(this.parseUser(res.sub, res.roles));
      })
    );
  }

  requestAccessToken(code): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(
      this.TOKEN_URI,
      this.getTokenRequestParams(code),
      { headers: this.getTokenRequestHeaders() }
    );
  }

  isAuthorized() {
    return !this.jwtHelper.isTokenExpired(AuthService.getAccessToken());
  }

  getBasicCredentials(user: string, password: string): string {
    return 'Basic ' + btoa(`${user}:${password}`);
  }

  parseUser(username: string, roles: string[]): User {
    return { username: username, name: '', roles: roles };
  }

  getTokenRequestHeaders() {
    const basicCreds = this.getBasicCredentials(
      environment.appClientId,
      environment.appClientSecret
    );
    return new HttpHeaders()
      .set('Authorization', basicCreds)
      .set('Content-Type', this.DefaultRequestContentType);
  }

  getTokenRequestParams(code?: string) {
    return new HttpParams()
      .set('grant_type', environment.authorizationGrantType)
      .set('redirect_uri', window.location.href.split('?')[0])
      .set('code', code)
  }
}
