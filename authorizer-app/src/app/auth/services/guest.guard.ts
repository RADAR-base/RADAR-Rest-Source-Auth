import { Injectable } from "@angular/core";
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from "@angular/router";
import { Observable } from "rxjs";
import { map } from "rxjs/operators";

import {AuthService} from "@app/auth/services/auth.service";
import {StorageItem} from "@app/auth/enums/storage-item";

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
          const lastLocation = JSON.parse(localStorage.getItem(StorageItem.LAST_LOCATION) || '{}');
          this.router.navigate(
            [lastLocation.url || '/'],
            {queryParams: lastLocation.params}
          ).finally();
        }
        return !loggedIn;
      })
    );
  }
}
