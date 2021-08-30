import {Injectable } from '@angular/core';
import {HttpEvent, HttpHandler, HttpInterceptor, HttpRequest} from '@angular/common/http';
import {Observable} from 'rxjs';
import {AuthService} from './auth.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

    constructor(
            private authService: AuthService) {}

    intercept(request: HttpRequest<any>, next: HttpHandler): Observable<HttpEvent<any>> {
      console.log('AuthInterceptor', this.authService.isAuthorized());
        if (this.authService.isAuthorized()) {
            const token: string = AuthService.getAccessToken() as string;
            console.log('token', token);
            request = request.clone({
                setHeaders: {
                    Authorization: 'Bearer ' + token
                }
            });

        }
        return next.handle(request);
    }
}
