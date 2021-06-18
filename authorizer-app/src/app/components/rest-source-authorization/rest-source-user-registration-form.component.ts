import { Component, OnInit } from '@angular/core';

import { FormBuilder } from '@angular/forms';
import { PlatformLocation } from '@angular/common';
import { RestSourceUserService } from '../../services/rest-source-user.service';
import { SourceClientAuthorizationService } from '../../services/source-client-authorization.service';

@Component({
  selector: 'rest-source-user-registration-form',
  templateUrl: './rest-source-user-registration-form.component.html'
})
export class RestSourceUserRegistrationFormComponent implements OnInit {
  sourceTypes: string[];
  selectedSourceType = '';

  constructor(
    private sourceClientAuthorizationService: SourceClientAuthorizationService,
    private platformLocation: PlatformLocation
  ) {}

  ngOnInit(): void {
    this.sourceClientAuthorizationService.getDeviceTypes().subscribe(data => {
      this.sourceTypes = data;
      if (!this.selectedSourceType && data.length > 0) {
        this.selectedSourceType = data[0];
      }
    });
  }

  requestToAuthorize() {
    const callbackUrl =
      window.location.origin +
      this.platformLocation.getBaseHrefFromDOM() +
      'users:new';

    return this.sourceClientAuthorizationService
      .getAuthorizationEndpoint(this.selectedSourceType, callbackUrl)
      .subscribe(url => (window.location.href = url));
  }
}
