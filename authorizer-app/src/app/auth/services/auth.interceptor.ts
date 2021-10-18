import {Injectable } from '@angular/core';
import {Router} from "@angular/router";
import {HttpErrorResponse, HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {BehaviorSubject, Observable, throwError} from 'rxjs';
import {catchError, filter, switchMap, take} from "rxjs/operators";

import {AuthService} from '@app/auth/services/auth.service';
import {AuthResponse} from "@app/auth/models/auth.model";
import {AuthStateCommand} from "@app/auth/enums/auth-state-command";
import {AUTH_ROUTE} from "@app/auth/auth-routing.module";

import {environment} from "@environments/environment";

@Injectable({
  providedIn: "root"
})
export class AuthInterceptor implements HttpInterceptor {
  private isRefreshing = false;
  private refreshTokenSubject: BehaviorSubject<any> = new BehaviorSubject<any>(null);

  constructor(private authService: AuthService, private router: Router) {}

  intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
    if(!request.url.startsWith(environment.backendBaseUrl) && !request.url.startsWith(environment.authBaseUrl)){
      return next.handle(request);
    }

    const token = AuthService.getAccessToken();
    if (token) {
      request = AuthInterceptor.addToken(request, token);
    }

    return next.handle(request).pipe(
      catchError(error => {
        if (error instanceof HttpErrorResponse && error.status === 401) {
          if (request.url === `${environment.authBaseUrl}/token`) {
            this.authService.logout();
            this.router.navigate([AUTH_ROUTE.LOGIN], {state: {command: AuthStateCommand.SESSION_EXPIRED}}).finally();
          } else {
            return this.handle401Error(request, next);
          }
        }
        return throwError(error);
      }
    ));
  }

  private handle401Error(request: HttpRequest<any>, next: HttpHandler) {
    if (!this.isRefreshing) {
      this.isRefreshing = true;
      this.refreshTokenSubject.next(null);

      return this.authService.refreshToken().pipe(
        switchMap((authResponse: AuthResponse) => {
          this.isRefreshing = false;
          this.refreshTokenSubject.next(authResponse.access_token);
          return next.handle(AuthInterceptor.addToken(request, authResponse.access_token));
        }),
        catchError(err => {
          return throwError(err);
        })
      );
    } else {
      return this.refreshTokenSubject.pipe(
        filter(token => token != null),
        take(1),
        switchMap(jwt => {
          return next.handle(AuthInterceptor.addToken(request, jwt));
        }));
    }
  }

  private static addToken(request: HttpRequest<any>, token: string) {
    return request.clone({
      setHeaders: {
        'Authorization': `Bearer ${token}`
      }
    });
  }
}
