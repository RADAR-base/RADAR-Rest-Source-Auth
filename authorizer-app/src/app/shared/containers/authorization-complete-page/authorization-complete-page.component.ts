import {Component, OnInit} from '@angular/core';
import {ActivatedRoute, Router} from '@angular/router';

import {UserService} from "@app/admin/services/user.service";
import {AuthService} from "@app/auth/services/auth.service";
import {StorageItem as SharedStorageItem} from "@app/shared/enums/storage-item";
import {StorageItem} from "@app/auth/enums/storage-item";

@Component({
  selector: 'app-authorization-complete-page',
  templateUrl: './authorization-complete-page.component.html',
  styleUrls: ['./authorization-complete-page.component.scss'],
})
export class AuthorizationCompletePageComponent implements OnInit {
  isLoading = true;
  error?: string;

  sourceType?: string;
  project?: string;

  constructor(
    private activatedRoute: ActivatedRoute,
    private router: Router,
    private service: UserService,
    public authService: AuthService
  ) {}

  ngOnInit(): void {
    this.isLoading = true;
    const queryParams = this.activatedRoute.snapshot.queryParams;
    const storedParams = this.service.getUserAuthParams();
    const stateOrToken = this.getStateOrToken(queryParams, storedParams);

    if (!stateOrToken) {
      this.handleError('SHARED.AUTHORIZATION_COMPLETE_PAGE.ERROR.badUrl');
      return;
    }

    const authorizeRequest = this.buildAuthorizeRequest(queryParams, storedParams);
    this.service.authorizeUser(authorizeRequest, stateOrToken).subscribe({
      next: (resp) => this.handleSuccessResponse(resp),
      error: (error) => this.handleError(error.error?.error_description || error.message || error)
    });
  }

  private getStateOrToken(queryParams: any, storedParams: any): string | null {
    return (
      this.getOrDefault(queryParams.state, storedParams.state) ||
      localStorage.getItem(SharedStorageItem.AUTHORIZATION_TOKEN)
    );
  }

  private buildAuthorizeRequest(queryParams: any, storedParams: any): any {
    return {
      code: this.getOrDefault(queryParams.code, storedParams.code),
      oauth_token: this.getOrDefault(queryParams.oauth_token, storedParams.oauth_token),
      oauth_verifier: this.getOrDefault(queryParams.oauth_verifier, storedParams.oauth_verifier),
      oauth_token_secret: this.getOrDefault(queryParams.oauth_token_secret, storedParams.oauth_token_secret)
    };
  }

  private handleSuccessResponse(resp: any): void {
    this.sourceType = resp.sourceType;
    this.project = resp.project.id;

    this.redirectToExternalUrl();

    if (resp.persistent) {
      this.isLoading = false;
      return;
    }
    const lastLocation = JSON.parse(localStorage.getItem(StorageItem.LAST_LOCATION) || '{}');
    this.router.navigate([lastLocation.url || '/'], { queryParams: lastLocation.params })
      .finally(() => this.service.clearUserAuthParams());
  }

  private redirectToExternalUrl(): void {
    const externalUrl = this.service.getReturnUrl();
    if (externalUrl) {
      this.service.clearReturnUrl();
      window.location.href = externalUrl;
    }
  }

  private handleError(message: string): void {
    this.error = message;
    this.isLoading = false;
  }

  private getOrDefault(value: any, defaultValue: any): any {
    return value ?? defaultValue;
  }
}
