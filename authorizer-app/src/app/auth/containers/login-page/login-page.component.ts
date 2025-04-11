import {Component, OnDestroy, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';
import {Subscription} from 'rxjs';

import {AuthService} from '@app/auth/services/auth.service';
import {GrantType} from '@app/auth/enums/grant-type';
import {AuthStateCommand} from "@app/auth/enums/auth-state-command";
import {StorageItem} from "@app/auth/enums/storage-item";
import {MessageBoxType} from "@app/shared/components/message-box/message-box.component";

import {environment} from '@environments/environment';
import {first} from "rxjs/operators";

@Component({
  selector: 'app-login-page',
  templateUrl: './login-page.component.html',
  styleUrls: ['./login-page.component.scss']
})
export class LoginPageComponent implements OnInit, OnDestroy {

  isLoading = false;
  error?: string;

  AuthStateCommand = AuthStateCommand;
  stateCommand?: AuthStateCommand;

  routerSubscription?: Subscription;

  MessageBoxType = MessageBoxType

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
          this.isLoading = true;
          this.authService
            .authenticate(code)
            .pipe(first())
            .subscribe({
              next: () => {
                const lastLocation = JSON.parse(localStorage.getItem(StorageItem.LAST_LOCATION) || '{}');
                this.router.navigate(
                  [lastLocation.url || '/'],
                  {queryParams: lastLocation.params}
                ).then(() => this.authService.clearLastLocation());
              },
              error: (error) => this.error = error.error?.error_description || error.message || error
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
      this.isLoading = true;
      this.redirectToAuthRequestLink();
    }
  }

  redirectToAuthRequestLink() {
    const scopes = "SOURCETYPE.READ%20PROJECT.READ%20SUBJECT.READ%20SUBJECT.UPDATE"
    window.location.href = `${environment.authBaseUrl}/auth?client_id=${
      environment.appClientId
    }&response_type=code&state=${Date.now()}&audience=res_restAuthorizer&scope=${scopes}&redirect_uri=${window.location.href.split('?')[0]}`;
  }
}
