import { Injectable } from "@angular/core";
import { CanActivate, ActivatedRouteSnapshot, RouterStateSnapshot, Router } from "@angular/router";
import { Observable } from "rxjs";
import { tap } from "rxjs/operators";
import {AuthService} from "./auth.service";
import {AuthStateCommand} from "../enums/auth-state-command";
import {StorageItem} from "../enums/storage-item";

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
          localStorage.setItem(StorageItem.RETURN_URL, state.url)
          this.router.navigate(['/login'], {state: {command: AuthStateCommand.AUTH_GUARD}}).finally();
        }
      })
    )
  }
}
