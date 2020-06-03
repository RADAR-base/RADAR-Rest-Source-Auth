import { AuthResponse, User } from '../models/auth.model';

import { Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { storageItems } from '../enums/storage';

@Injectable()
export class AuthService {
  constructor() {}

  static getAccessToken(): string {
    return localStorage.getItem(storageItems.accessToken);
  }

  static getUser(): User {
    const user = localStorage.getItem(storageItems.user);
    return JSON.parse(user);
  }

  setAccessToken(token: string) {
    localStorage.setItem(storageItems.accessToken, token);
  }

  setUser(user: User) {
    localStorage.setItem(storageItems.user, JSON.stringify(user));
  }

  clearAccessToken() {
    localStorage.removeItem(storageItems.accessToken);
  }

  clearUser() {
    localStorage.removeItem(storageItems.user);
  }

  clearAuth() {
    this.clearAccessToken();
    this.clearUser();
  }

  authenticate(loginParams): Observable<any> {
    throw new Error('AuthService method not implemented');
  }

  requestAccessToken(params): Observable<AuthResponse> {
    throw new Error('AuthService method not implemented');
  }

  isAuthorized(): boolean {
    throw new Error('AuthService method not implemented');
  }
}
