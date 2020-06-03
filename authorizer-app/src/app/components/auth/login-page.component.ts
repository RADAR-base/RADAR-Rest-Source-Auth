import { ActivatedRoute, Router } from '@angular/router';
import { Component, OnDestroy, OnInit } from '@angular/core';

import { AuthService } from '../../services/auth.service';
import { Subscription } from 'rxjs';
import { environment } from 'src/environments/environment';
import { grantType } from 'src/app/enums/grant-type';

@Component({
  selector: 'login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.css']
})
export class LoginPageComponent implements OnInit, OnDestroy {
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  routerSubscription: Subscription;
  authCodeSubscription: Subscription;

  ngOnInit() {
    if (environment.authorizationGrantType == grantType.AUTHORIZATION_CODE) {
      this.loginWithAuthCode();
    }
  }

  ngOnDestroy() {
    this.authCodeSubscription.unsubscribe();
    this.routerSubscription.unsubscribe();
  }

  loginHandler() {
    if (environment.authorizationGrantType == grantType.AUTHORIZATION_CODE) {
      this.redirectToAuthRequestLink();
    }
  }

  redirectToAuthRequestLink() {
    window.location.href = `${environment.authBaseUrl}/authorize?client_id=${
      environment.appClientId
    }&response_type=code&redirect_uri=${window.location.href.split('?')[0]}`;
  }

  loginWithAuthCode() {
    this.routerSubscription = this.route.queryParams.subscribe(params => {
      if (params.code) {
        this.authCodeSubscription = this.authService
          .authenticate(params.code)
          .subscribe(() => this.router.navigate(['/']));
      }
    });
  }
}
