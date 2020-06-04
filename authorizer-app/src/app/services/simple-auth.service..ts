import { AuthService } from './auth.service';
import { Injectable } from '@angular/core';
import { of } from 'rxjs';

@Injectable()
export class SimpleAuthService extends AuthService {
  constructor() {
    super();
  }

  authenticate(authCode?) {
    return of(true);
  }

  isAuthorized() {
    return true;
  }
}
