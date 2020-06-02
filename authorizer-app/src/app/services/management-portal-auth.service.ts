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
  TOKEN_URI = `${environment.AUTH_URI}/token`;

  constructor(private http: HttpClient, private jwtHelper: JwtHelperService) {
    super();
  }
  authenticate(authCode) {
    return this.requestAccessToken(authCode).pipe(
      map(res => {
        this.setAccessToken(res.access_token);
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
      environment.AUTH.client_id,
      environment.AUTH.client_secret
    );
    return new HttpHeaders()
      .set('Authorization', basicCreds)
      .set('Content-Type', this.DefaultRequestContentType);
  }

  getTokenRequestParams(code?: string) {
    return new HttpParams()
      .set('grant_type', environment.AUTH.grant_type)
      .set('redirect_uri', window.location.href.split('?')[0])
      .set('code', code);
  }
}
