import { CanActivate, Router } from '@angular/router';

import { AuthService } from './auth.service';
import { Injectable } from '@angular/core';

@Injectable()
export class AuthGuard implements CanActivate {
  constructor(private router: Router, private authService: AuthService) {}

  canActivate(): boolean {
    console.log('Auth Guard');
    if (!this.authService.isAuthorized()) {
      this.router.navigate(['/login']);
      return false;
    }
    return true;
  }
}
