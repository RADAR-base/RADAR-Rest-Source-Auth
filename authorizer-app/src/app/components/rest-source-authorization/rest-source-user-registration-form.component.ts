import { Component, OnInit } from '@angular/core';

import { FormBuilder } from '@angular/forms';
import { PlatformLocation } from '@angular/common';
import { RestSourceUserService } from '../../services/rest-source-user.service';
import { SourceClientAuthorizationService } from '../../services/source-client-authorization.service';
import { RestSourceClientDetails } from '../../models/source-client-details.model';

@Component({
  selector: 'rest-source-user-registration-form',
  templateUrl: './rest-source-user-registration-form.component.html'
})
export class RestSourceUserRegistrationFormComponent implements OnInit {
  sourceTypes: RestSourceClientDetails[];
  selectedSourceType?: RestSourceClientDetails = null;

  constructor(
    private sourceClientAuthorizationService: SourceClientAuthorizationService,
    private platformLocation: PlatformLocation
  ) {}

  ngOnInit(): void {
    this.sourceClientAuthorizationService.getDeviceTypes()
      .subscribe(data => {
        this.sourceTypes = data.sourceClients;
        if (!this.selectedSourceType && this.sourceTypes.length > 0) {
          this.selectedSourceType = this.sourceTypes[0];
        }
      });
  }

  requestToAuthorize() {
    if (this.selectedSourceType == null) {
      return;
    }
    const callbackUrl =
      window.location.origin +
      this.platformLocation.getBaseHrefFromDOM() +
      'users:new';

    return this.sourceClientAuthorizationService
      .getAuthorizationEndpoint(this.selectedSourceType, callbackUrl)
      .subscribe(url => (window.location.href = url));
  }
}
