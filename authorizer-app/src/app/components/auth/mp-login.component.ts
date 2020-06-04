import {
  Component,
  EventEmitter,
  Input,
  OnDestroy,
  OnInit,
  Output
} from '@angular/core';

import { Subscription } from 'rxjs';
import { environment } from 'src/environments/environment';

@Component({
  selector: 'mp-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login-page.component.css']
})
export class MpLoginComponent implements OnInit, OnDestroy {
  buttonDisabled = false;
  buttonLabel = 'LOG IN WITH MANAGEMENT PORTAL';
  routerSubscription = new Subscription();

  @Input()
  queryParams;
  @Output()
  authenticate: EventEmitter<string> = new EventEmitter<string>();

  constructor() {}

  ngOnInit() {
    this.loginWithAuthCode();
  }

  ngOnDestroy() {
    this.routerSubscription.unsubscribe();
  }

  loginHandler() {
    this.redirectToAuthRequestLink();
  }

  redirectToAuthRequestLink() {
    window.location.href = `${environment.authBaseUrl}/authorize?client_id=${
      environment.appClientId
    }&response_type=code&redirect_uri=${window.location.href.split('?')[0]}`;
  }

  loginWithAuthCode() {
    if (this.queryParams.code) {
      this.buttonDisabled = true;
      this.buttonLabel = 'LOGGING IN..';
      this.authenticate.emit(this.queryParams.code);
    }
  }
}
