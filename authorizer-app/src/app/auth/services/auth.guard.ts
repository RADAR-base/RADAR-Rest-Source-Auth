import { Injectable } from "@angular/core";
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from "@angular/router";
import { Observable } from "rxjs";
import { tap } from "rxjs/operators";

import {AuthService} from "@app/auth/services/auth.service";
import {AuthStateCommand} from "@app/auth/enums/auth-state-command";
import {AUTH_ROUTE} from "@app/auth/auth-routing.module";

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private authService: AuthService, private router: Router) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.authService.isAuthorized().pipe(
      tap(loggedIn => {
        if (!loggedIn) {
          this.router.navigate(
            [AUTH_ROUTE.LOGIN],
            {state: {command: AuthStateCommand.AUTH_GUARD}}
          ).finally();
        }
      })
    )
  }
}
