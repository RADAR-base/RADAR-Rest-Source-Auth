import { Injectable } from '@angular/core';
import {Router} from "@angular/router";
import { HttpRequest, HttpHandler, HttpEvent, HttpInterceptor } from '@angular/common/http';
import { Observable, throwError } from 'rxjs';
import { catchError } from 'rxjs/operators';

import {AuthService} from "@app/auth/services/auth.service";
import {AUTH_ROUTE} from "@app/auth/auth-routing.module";

@Injectable()
export class ErrorInterceptor implements HttpInterceptor {
    constructor(private authService: AuthService, private router: Router) { }

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
        return next.handle(request).pipe(catchError(err => {
            if ([401, 403].indexOf(err.status) !== -1) {
                this.authService.clearAccessToken();
                this.router.navigate([AUTH_ROUTE.LOGIN]).finally();
            }
            return throwError(err);
        }));
    }
}
