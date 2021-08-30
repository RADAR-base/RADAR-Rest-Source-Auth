import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { AuthService } from './auth.service';
import { Injectable } from '@angular/core';
import { JwtHelperService } from '@auth0/angular-jwt';
import { Observable } from 'rxjs';
import { environment } from 'src/environments/environment';
import { map } from 'rxjs/operators';
import {AuthResponse, User} from '../models/auth.model';

@Injectable()
export class ManagementPortalAuthService extends AuthService {
  DefaultRequestContentType = 'application/x-www-form-urlencoded';
  TOKEN_URI = `${environment.authBaseUrl}/token`;

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
      .set('code', code);
  }
}
