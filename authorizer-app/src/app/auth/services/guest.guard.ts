import { Injectable } from "@angular/core";
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from "@angular/router";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";

import {AuthService} from "@app/auth/services/auth.service";

@Injectable()
export class GuestGuard implements CanActivate {
  constructor(private router: Router, private authService: AuthService) { }

  canActivate(
    route: ActivatedRouteSnapshot,
    state: RouterStateSnapshot
  ): Observable<boolean> | Promise<boolean> | boolean {
    return this.authService.isAuthorized().pipe(
      map(loggedIn => {
        if (loggedIn) {
          this.router.navigate(['/']).finally();
        }
        return !loggedIn;
      })
    );
  }
}
