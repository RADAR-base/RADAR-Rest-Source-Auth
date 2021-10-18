import { Injectable } from '@angular/core';
import {Router} from "@angular/router";
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';
import { JwtHelperService } from '@auth0/angular-jwt';
import {BehaviorSubject, Observable, of} from 'rxjs';
import {catchError, map, shareReplay, tap} from 'rxjs/operators';

import { AuthService } from '@app/auth/services/auth.service';
import {AuthResponse, User} from '@app/auth/models/auth.model';
import {AUTH_ROUTE} from "@app/auth/auth-routing.module";

import {environment} from "@environments/environment";

@Injectable()
export class ManagementPortalAuthService extends AuthService {
  DefaultRequestContentType = 'application/x-www-form-urlencoded';
  TOKEN_URI = `${environment.authBaseUrl}/token`;

  private subject = new BehaviorSubject<User | null>(null);
  user$ : Observable<User | null> = this.subject.asObservable();

  constructor(private http: HttpClient, private jwtHelper: JwtHelperService, private router: Router) {
    super();

    this.isLoggedIn$ = this.user$.pipe(map(user => !!user));
    this.isLoggedOut$ = this.isLoggedIn$.pipe(map(loggedIn => !loggedIn));

    const user = this.getUser();

    if (user) {
      this.subject.next(user);
    }
  }

  authenticate(authCode: string): Observable<any> {
    return this.requestAccessToken(authCode);
  }

  requestAccessToken(authCode: string): Observable<AuthResponse> {
    const url = this.TOKEN_URI;
    const payload = this.getAccessTokenRequestParams(authCode);
    const options = { headers: this.getTokenRequestHeaders() };
    return this.http.post<AuthResponse>(url, payload, options).pipe(
      tap(authResponse => {
        this.subject.next(this.parseUser(authResponse.sub, authResponse.roles));
        this.setAuth(authResponse)
      }),
      shareReplay()
    );
  }

  refreshToken(): Observable<AuthResponse> {
    const url = this.TOKEN_URI;
    const payload = this.getRefreshTokenRequestParams();
    const options = { headers: this.getTokenRequestHeaders() };
    return this.http.post<AuthResponse>(url, payload, options).pipe(
      tap((authResponse: AuthResponse) => {
        this.setAuth(authResponse)
      })
    );
  }

  logout() {
    this.subject.next(null);

    this.clearAuth();
    this.router.navigate([AUTH_ROUTE.LOGIN]).finally();
  }

  isAuthorized(): Observable<boolean> {
    const accessToken = AuthService.getAccessToken();
    if (!accessToken) {
      return of(false);
    }
    if(!this.jwtHelper.isTokenExpired(<string>accessToken)){
      return of(true);
    } else {
      return this.refreshToken().pipe(
        map(() => true),
        catchError(() => of(false))
      )
    }
  }

  getAccessTokenRequestParams(authCode: string) {
    return new HttpParams()
      .set('grant_type', environment.authorizationGrantType)
      .set('redirect_uri', window.location.href.split('?')[0])
      .set('code', authCode)
  }

  getTokenRequestHeaders() {
    const basicCredentials = this.getBasicCredentials(
      environment.appClientId,
      environment.appClientSecret
    );
    return new HttpHeaders()
      .set('Authorization', basicCredentials)
      .set('Content-Type', this.DefaultRequestContentType);
  }

  getRefreshTokenRequestParams() {
    return new HttpParams()
      .set("grant_type", "refresh_token")
      .set("refresh_token", this.getRefreshToken() || '')
      .set("client_id", environment.appClientId)
      .set("client_secret", environment.appClientSecret);
  }

  getBasicCredentials(user: string, password: string): string {
    return 'Basic ' + btoa(`${user}:${password}`);
  }
}
