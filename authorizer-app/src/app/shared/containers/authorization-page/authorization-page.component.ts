import {Component, OnInit} from '@angular/core';
import {ActivatedRoute} from '@angular/router';

import {UserService} from "@app/admin/services/user.service";
import {StorageItem} from "@app/shared/enums/storage-item";

@Component({
  selector: 'app-authorization-page',
  templateUrl: './authorization-page.component.html',
  styleUrls: ['./authorization-page.component.scss'],
})
export class AuthorizationPageComponent implements OnInit {
  isLoading = true;
  error?: any;

  sourceType?: string;
  project?: string;
  authEndpointUrl?: string;

  constructor(
    private activatedRoute: ActivatedRoute,
    private userService: UserService,
  ) {}

  ngOnInit(): void {
    const {token, secret, redirect, return_to} = this.activatedRoute.snapshot.queryParams;
    if(!token || !secret){
      this.error = 'SHARED.AUTHORIZATION_PAGE.ERROR.badUrl';
      this.isLoading = false;
      return;
    }
    this.userService.storeAuthorizationToken(token);
    this.userService.storeReturnUrl(return_to);
    this.userService.getAuthEndpointUrl({secret}, token).subscribe({
      next: (resp) => {
        if (resp.authEndpointUrl) {
          this.sourceType = resp.sourceType;
          this.project = resp.project.id;
          this.authEndpointUrl = resp.authEndpointUrl;
          this.userService.storeUserAuthParams(resp.authEndpointUrl);
          if (redirect) {
            this.authorize();
          }
          this.isLoading = false;
        }
      },
      error: (error) => {
        this.isLoading = false;
        if(error.status === 400 && error.error.error === 'registration_not_found'){
          this.error = 'SHARED.AUTHORIZATION_PAGE.ERROR.registrationNotFound';
          return;
        }
        if(error.status === 400 && error.error.error === 'bad_secret'){
          this.error = 'SHARED.AUTHORIZATION_PAGE.ERROR.badSecret';
          return;
        }
        this.error = error.error?.error_description || error.message || error;
      },
    });
  }

  authorize(): void {
    if(this.authEndpointUrl) {
      window.location.href = this.authEndpointUrl;
    }
  }
}
