import { Component, OnInit } from "@angular/core";

import { FormBuilder } from "@angular/forms";
import { PlatformLocation } from "@angular/common";
import { RestSourceClientDetails } from "../../../../models/source-client-details.model";
import { RestSourceUserService } from "../../../../services/rest-source-user.service";
import { SourceClientAuthorizationService } from "../../../../services/source-client-authorization.service";

@Component({
  selector: "rest-source-user-registration-form",
  templateUrl: "./rest-source-user-registration-form.component.html"
})
export class RestSourceUserRegistrationFormComponent implements OnInit {
  sourceTypeForm: any;
  callbackUrl: String;

  sourceTypes: string[];
  sourceClientDetail: RestSourceClientDetails;

  constructor(
    private restSourceUserService: RestSourceUserService,
    private sourceClientAuthorizationService: SourceClientAuthorizationService,
    private fb: FormBuilder,
    private platformLocation: PlatformLocation
  ) {
    this.createForm();
  }

  ngOnInit(): void {
    this.sourceClientAuthorizationService.getDeviceTypes().subscribe(data => {
      this.sourceTypes = data;
    });
  }

  createForm() {
    this.sourceTypeForm = this.fb.group({
      selectedDeviceType: ""
    });
  }

  onChange(sourceType: any) {
    this.sourceClientAuthorizationService
      .getSourceClientAuthDetails(sourceType)
      .subscribe(data => {
        this.sourceClientDetail = data;
        this.callbackUrl =
          window.location.origin +
          this.platformLocation.getBaseHrefFromDOM() +
          "users:new";
      });
  }
}
