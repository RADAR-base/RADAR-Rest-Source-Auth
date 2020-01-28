import { AuthData, AuthResponse, User } from '../models/auth.model';
import { HttpClient, HttpHeaders, HttpParams } from '@angular/common/http';

import { Injectable } from '@angular/core';
import { environment } from 'src/environments/environment';
import { map } from 'rxjs/operators';
import { of } from 'rxjs';
import { storageItems } from '../enums/storage';

@Injectable()
export class AuthService {
  DefaultRequestEncodedContentType = 'application/x-www-form-urlencoded';
  TOKEN_URI = `${environment.AUTH_URI}/token`;

  constructor(private http: HttpClient) {}

  static getToken(): string {
    return localStorage.getItem(storageItems.token);
  }

  static getUser(): User {
    const user = localStorage.getItem(storageItems.user);
    return JSON.parse(user);
  }

  static basicCredentials(user: string, password: string): string {
    return 'Basic ' + btoa(`${user}:${password}`);
  }

  getAuthData(): AuthData {
    const token = AuthService.getToken();
    const user = AuthService.getUser();
    return { token, user };
  }

  setAuthData({ token, user }) {
    localStorage.setItem(storageItems.token, token);
    localStorage.setItem(storageItems.user, JSON.stringify(user));
  }

  clearAuthData() {
    localStorage.removeItem(storageItems.token);
    localStorage.removeItem(storageItems.user);
  }

  login(code) {
    return this.authenticateUser(code).pipe(map(res => this.setAuthData(res)));
  }

  authenticateUser(code) {
    return this.http
      .post<AuthResponse>(this.TOKEN_URI, this.getAuthParams(code), {
        headers: this.getAuthHeaders()
      })
      .pipe(
        map((response: any) => ({
          token: response.access_token,
          user: this.parseUser(response.sub, response.roles)
        }))
      );
  }

  parseUser(username: string, roles: string[]): User {
    return { username: username, name: '', roles: roles };
  }

  logout() {
    return of(true);
  }

  getAuthHeaders() {
    const basicCreds = AuthService.basicCredentials(
      environment.AUTH.client_id,
      environment.AUTH.client_secret
    );
    return new HttpHeaders()
      .set('Authorization', basicCreds)
      .set('Content-Type', this.DefaultRequestEncodedContentType);
  }

  getAuthParams(code?: string) {
    return new HttpParams()
      .set('grant_type', environment.AUTH.grant_type)
      .set('redirect_uri', window.location.href.split('?')[0])
      .set('code', code);
  }
}
