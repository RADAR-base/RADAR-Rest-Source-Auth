import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { AuthResponse, User } from '@app/auth/models/auth.model';
import {StorageItem} from "@app/auth/enums/storage-item";

@Injectable()
export class AuthService {
  isLoggedIn$: Observable<boolean> | undefined;
  isLoggedOut$: Observable<boolean> | undefined;

  constructor() {}

  setAuth(authResponse: AuthResponse): void {
    this.setAccessToken(authResponse.access_token);
    this.setRefreshToken(authResponse.refresh_token);
    this.setUser(this.parseUser(authResponse.sub, authResponse.roles));
  }

  clearAuth() {
    this.clearAccessToken();
    this.clearRefreshToken();
    this.clearUser();
  }

  static getAccessToken() {
    return localStorage.getItem(StorageItem.ACCESS_TOKEN);
  }

  setAccessToken(token: string): void {
    localStorage.setItem(StorageItem.ACCESS_TOKEN, token);
  }

  clearAccessToken(): void {
    localStorage.removeItem(StorageItem.ACCESS_TOKEN);
  }

  getRefreshToken() {
    return localStorage.getItem(StorageItem.REFRESH_TOKEN);
  }

  setRefreshToken(token: string): void {
    localStorage.setItem(StorageItem.REFRESH_TOKEN, token);
  }

  clearRefreshToken(): void {
    localStorage.removeItem(StorageItem.REFRESH_TOKEN);
  }

  getUser(): User {
    const user = localStorage.getItem(StorageItem.USER);
    return user? JSON.parse(user) : null;
  }

  setUser(user: User): void {
    localStorage.setItem(StorageItem.USER, JSON.stringify(user));
  }

  clearUser(): void {
    localStorage.removeItem(StorageItem.USER);
  }

  parseUser(username: string, roles: string[]): User {
    return { username: username, name: '', roles: roles };
  }

  clearLastLocation(): void {
    localStorage.removeItem(StorageItem.LAST_LOCATION);
    // localStorage.removeItem(StorageItem.SAVED_PARAMS);
    // localStorage.removeItem(StorageItem.SAVED_URL);
  }

  authenticate(loginParams: any): Observable<any> {
    throw new Error('AuthService method not implemented');
  }

  requestAccessToken(params: any): Observable<AuthResponse> {
    throw new Error('AuthService method not implemented');
  }

  isAuthorized(): Observable<boolean> {
    throw new Error('AuthService method not implemented');
  }

  logout(): void {
    throw new Error('AuthService method not implemented');
  }

  refreshToken(): Observable<AuthResponse> {
    throw new Error('AuthService method not implemented');
  }
}
