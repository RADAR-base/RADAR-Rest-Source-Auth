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
    const {state, oauth_token, oauth_verifier, oauth_token_secret, code} = this.activatedRoute.snapshot.queryParams;

    let stateOrToken = state;
    if (!state) {
      stateOrToken = localStorage.getItem(SharedStorageItem.AUTHORIZATION_TOKEN);
    }
    if(!stateOrToken){
      this.error = 'SHARED.AUTHORIZATION_COMPLETE_PAGE.ERROR.badUrl';
      this.isLoading = false;
      return;
    }
    const authorizeRequest = {
      code,
      oauth_token,
      oauth_verifier,
      oauth_token_secret
    };
    this.service.authorizeUser(authorizeRequest, stateOrToken).subscribe({
      next: (resp) => {
        this.sourceType = resp.sourceType;
        this.project = resp.project.id;
        if (resp.persistent) {
          this.isLoading = false;
        } else {
          const lastLocation = JSON.parse(localStorage.getItem(StorageItem.LAST_LOCATION) || '{}');
          this.router.navigate(
            [lastLocation.url || '/'],
            {queryParams: lastLocation.params}
          ).finally();
        }
      },
      error: (error) => {
        this.isLoading = false;
        // TODO translate errors
        this.error = error.error?.error_description || error.message || error;
      }
    });
  }
}