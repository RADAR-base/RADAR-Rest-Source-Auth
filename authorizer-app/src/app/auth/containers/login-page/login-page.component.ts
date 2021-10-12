import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {first, Subscription} from 'rxjs';

import {AuthService} from '@app/auth/services/auth.service';
import {GrantType} from '@app/auth/enums/grant-type';
import {AuthStateCommand} from "@app/auth/enums/auth-state-command";
import {StorageItem} from "@app/auth/enums/storage-item";

import {environment} from '@environments/environment';

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent implements OnInit, OnDestroy {

  loading = false;
  error?: any;

  AuthStateCommand = AuthStateCommand;
  stateCommand?: AuthStateCommand;

  routerSubscription?: Subscription;

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {
    const state = this.router.getCurrentNavigation()?.extras.state;
    if (state) {
      this.stateCommand = state.command;
    }
  }

  ngOnInit() {
    if (environment.authorizationGrantType === GrantType.AUTHORIZATION_CODE) {
      this.loginWithAuthCode();
    }
  }

  loginWithAuthCode() {
    this.routerSubscription = this.activatedRoute.queryParams.subscribe({
      next: params => {
        const {code} = params;
        if (code) {
          this.loading = true;
          this.authService
            .authenticate(code)
            .pipe(first())
            .subscribe({
              next: () => {
                const returnUrl = localStorage.getItem(StorageItem.RETURN_URL);
                if (returnUrl) {
                  this.router.navigateByUrl(returnUrl).then(() => this.authService.clearReturnUrl());
                } else {
                  this.router.navigateByUrl("/").then(() => this.authService.clearReturnUrl());
                }
              },
              error: (error) => this.error = error
            });
        }
      }
    });
  }

  ngOnDestroy() {
    this.routerSubscription?.unsubscribe();
  }

  loginHandler() {
    if (environment.authorizationGrantType === GrantType.AUTHORIZATION_CODE) {
      this.loading = true;
      this.redirectToAuthRequestLink();
    }
  }

  redirectToAuthRequestLink() {
    window.location.href = `${environment.authBaseUrl}/authorize?client_id=${
      environment.appClientId
    }&response_type=code&redirect_uri=${window.location.href.split('?')[0]}`;
  }
}
